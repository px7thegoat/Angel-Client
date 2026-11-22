package io.github.angelclient.client.cosmetic.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.angelclient.client.cosmetic.CosmeticAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class BlockbenchModelLoader {

    private static final Map<String, BlockbenchModel> cache = new HashMap<>();

    public static BlockbenchModel loadModel(String path) {
        BlockbenchModel cached = cache.get(path);
        if (cached != null) return cached;

        BlockbenchModel model = new BlockbenchModel();

        try {
            String json;
            ResourceLocation loc = new ResourceLocation("angelcosmetics", path);
            try {
                InputStream in = Minecraft.getMinecraft().getResourceManager().getResource(loc).getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                json = sb.toString();
            } catch (Exception e) {
                String url = CosmeticAPI.GITHUB_BASE_URL + path;
                byte[] data = CosmeticAPI.downloadBytes(url);
                if (data == null) {
                    System.err.println("[AngelClient] Failed to load Blockbench model: " + path);
                    cache.put(path, model);
                    return model;
                }
                json = new String(data, "UTF-8");
            }

            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            JsonArray elementsArr = root.getAsJsonArray("elements");
            if (elementsArr == null) {
                cache.put(path, model);
                return model;
            }

            for (JsonElement el : elementsArr) {
                JsonObject obj = el.getAsJsonObject();
                BlockbenchModel.Element elem = new BlockbenchModel.Element();

                JsonArray fromArr = obj.getAsJsonArray("from");
                JsonArray toArr = obj.getAsJsonArray("to");
                elem.from = new float[]{fromArr.get(0).getAsFloat(), fromArr.get(1).getAsFloat(), fromArr.get(2).getAsFloat()};
                elem.to = new float[]{toArr.get(0).getAsFloat(), toArr.get(1).getAsFloat(), toArr.get(2).getAsFloat()};

                if (obj.has("rotation")) {
                    JsonObject rot = obj.getAsJsonObject("rotation");
                    JsonArray origin = rot.getAsJsonArray("origin");
                    elem.rotationOrigin = new float[]{origin.get(0).getAsFloat(), origin.get(1).getAsFloat(), origin.get(2).getAsFloat()};
                    elem.rotationAxis = rot.get("axis").getAsString();
                    elem.rotationAngle = rot.get("angle").getAsFloat();
                }

                JsonObject faces = obj.getAsJsonObject("faces");
                for (Map.Entry<String, JsonElement> entry : faces.entrySet()) {
                    String dir = entry.getKey();
                    JsonObject faceObj = entry.getValue().getAsJsonObject();
                    JsonArray uv = faceObj.getAsJsonArray("uv");
                    float texSize = 16f;

                    BlockbenchModel.Face face = new BlockbenchModel.Face();
                    face.direction = dir;
                    face.u1 = uv.get(0).getAsFloat() / texSize;
                    face.v1 = uv.get(1).getAsFloat() / texSize;
                    face.u2 = uv.get(2).getAsFloat() / texSize;
                    face.v2 = uv.get(3).getAsFloat() / texSize;
                    if (faceObj.has("rotation")) {
                        face.uvRotation = faceObj.get("rotation").getAsInt();
                    }
                    elem.faces.add(face);
                }

                model.addElement(elem);
            }

            System.out.println("[AngelClient] Loaded Blockbench model: " + path + " (" + model.getElementCount() + " elements)");
        } catch (Exception e) {
            System.err.println("[AngelClient] Failed to parse Blockbench model: " + path);
            e.printStackTrace();
        }

        cache.put(path, model);
        return model;
    }

    public static void clearCache() {
        cache.clear();
    }
}
