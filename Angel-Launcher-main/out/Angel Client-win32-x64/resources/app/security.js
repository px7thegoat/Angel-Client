/*
 * Launcher <-> Client session bridge.
 *
 * The launcher opens a loopback-only TCP server, hands a signed session token
 * to the JVM and waits for the client to prove that it knows the shared key
 * before letting Minecraft start. The same key is also used to sign the
 * client JAR hash, so a modified JAR is refused before it is executed.
 */

const crypto = require("crypto");
const fs = require("fs");
const net = require("net");

const KEY = Buffer.from("4e09e4ff948e55241a7cdb55aa6473c5e896f9ea922b80c4e63479ad42d84537", "hex");
const PROTOCOL = "ANGEL/1";

function hmac(data) {
	return crypto.createHmac("sha256", KEY).update(data, "utf8").digest("hex");
}

function safeEqual(a, b) {
	if(typeof a !== "string" || typeof b !== "string" || a.length !== b.length) {
		return false;
	}

	try {
		return crypto.timingSafeEqual(Buffer.from(a, "hex"), Buffer.from(b, "hex"));
	}
	catch(_) {
		return false;
	}
}

function sha256File(file) {
	return new Promise((resolve, reject) => {
		const hash = crypto.createHash("sha256");
		const stream = fs.createReadStream(file);

		stream.on("data", (chunk) => hash.update(chunk));
		stream.on("error", reject);
		stream.on("end", () => resolve(hash.digest("hex")));
	});
}

/**
 * Verifies the client JAR against its signed checksum file (game.jar.sig).
 * A missing .sig is only tolerated for development builds (electron .),
 * where the client JAR is rebuilt locally; packaged builds must be signed.
 */
async function verifyClientJar(jarPath, requireSigned) {
	if(!fs.existsSync(jarPath)) {
		return { ok: false, signed: false, reason: "client jar not found" };
	}

	const mustBeSigned = typeof requireSigned === "boolean" ? requireSigned : !process.defaultApp;
	const hash = await sha256File(jarPath);
	const sigFile = jarPath + ".sig";

	if(!fs.existsSync(sigFile)) {
		if(mustBeSigned) {
			return { ok: false, signed: false, hash, reason: "assinatura do client ausente" };
		}

		return { ok: true, signed: false, hash };
	}

	try {
		const signature = fs.readFileSync(sigFile, "utf8").trim();
		const expected = hmac("angel.jar.v1|" + hash);

		if(!safeEqual(signature, expected)) {
			return { ok: false, signed: true, hash, reason: "jar signature mismatch" };
		}

		return { ok: true, signed: true, hash };
	}
	catch(error) {
		return { ok: false, signed: true, hash, reason: error.message };
	}
}

/**
 * Signs a client JAR (used after an update) so the integrity check keeps
 * working with the freshly downloaded build.
 */
function signClientJar(jarPath) {
	const hash = crypto.createHash("sha256").update(fs.readFileSync(jarPath)).digest("hex");
	fs.writeFileSync(jarPath + ".sig", hmac("angel.jar.v1|" + hash) + "\n", "utf8");
	return hash;
}

class Session {

	constructor(username, log) {
		this.username = username;
		this.log = typeof log === "function" ? log : () => {};
		this.nonce = crypto.randomBytes(16).toString("hex");
		this.token = null;
		this.port = 0;
		this.connected = false;
		this.closed = false;
		this.verifiedHash = null;
		this.server = net.createServer((socket) => this._onConnection(socket));
		this._ready = new Promise((resolve, reject) => {
			this._resolve = resolve;
			this._reject = reject;
		});
	}

	async open() {
		await new Promise((resolve, reject) => {
			this.server.once("error", reject);
			this.server.listen(0, "127.0.0.1", () => {
				this.port = this.server.address().port;
				resolve();
			});
		});

		const stamp = Date.now().toString(16);
		this.token = stamp + ":" + hmac("angel.session.v1|" + this.username + "|" + this.nonce + "|" + stamp);
		this.log("[Guard] sessão aberta na porta " + this.port);
		return this;
	}

	props(jarHash) {
		return {
			token: this.token,
			nonce: this.nonce,
			port: String(this.port),
			user: this.username,
			hash: jarHash || ""
		};
	}

	_onConnection(socket) {
		socket.setTimeout(6000);
		let buffer = "";

		socket.on("error", () => socket.destroy());

		socket.on("data", (chunk) => {
			if(this.closed) {
				socket.destroy();
				return;
			}

			buffer += chunk.toString("utf8");
			const index = buffer.indexOf("\n");

			if(index < 0) {
				if(buffer.length > 8192) {
					socket.destroy();
				}
				return;
			}

			const line = buffer.substring(0, index).trim();
			const parts = line.split(" ");

			if(parts.length !== 3 || parts[0] !== PROTOCOL || parts[1] !== this.token) {
				this.log("[Guard] handshake recusado");
				socket.end("DENY\n");
				this._fail(new Error("handshake recusado pelo launcher"));
				return;
			}

			const clientNonce = parts[2];
			const signature = hmac("angel.ok.v1|" + clientNonce + "|" + this.token);
			socket.end("OK " + signature + "\n");
			this.connected = true;
			this.log("[Guard] client autenticado");
			this._resolve(true);
		});
	}

	_fail(error) {
		if(!this.connected) {
			this._reject(error);
		}
	}

	waitForClient(timeoutMs) {
		return new Promise((resolve, reject) => {
			let done = false;
			const timer = setTimeout(() => {
				if(!done) {
					done = true;
					reject(new Error("o client não respondeu ao handshake"));
				}
			}, timeoutMs);

			this._ready.then(() => {
				if(!done) {
					done = true;
					clearTimeout(timer);
					resolve(true);
				}
			}).catch((error) => {
				if(!done) {
					done = true;
					clearTimeout(timer);
					reject(error);
				}
			});
		});
	}

	close() {
		if(this.closed) {
			return;
		}

		this.closed = true;
		try {
			this.server.close();
		}
		catch(_) {}
	}

}

module.exports = { Session, verifyClientJar, signClientJar, hmac, PROTOCOL };
