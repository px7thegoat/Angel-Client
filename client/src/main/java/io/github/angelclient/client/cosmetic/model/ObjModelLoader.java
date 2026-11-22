package io.github.angelclient.client.cosmetic.model;

import io.github.angelclient.client.cosmetic.CosmeticAPI;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ObjModelLoader {

    private static final Map<String, ModelOBJ> cache = new HashMap<>();

    public static ModelOBJ loadModel(String path) {
        ModelOBJ cached = cache.get(path);
        if (cached != null) return cached;

        ModelOBJ model = new ModelOBJ();
        String url = CosmeticAPI.GITHUB_BASE_URL + path;
        byte[] data = CosmeticAPI.downloadBytes(url);

        if (data == null) {
            System.err.println("[AngelClient] Failed to download OBJ model: " + url);
            cache.put(path, model);
            return model;
        }

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("o ") || line.startsWith("s ") || line.startsWith("usemtl") || line.startsWith("mtllib")) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                if (parts.length == 0) continue;

                switch (parts[0]) {
                    case "v":
                        model.addVertex(
                                Float.parseFloat(parts[1]),
                                Float.parseFloat(parts[2]),
                                Float.parseFloat(parts[3])
                        );
                        break;
                    case "vt":
                        model.addTexCoord(
                                Float.parseFloat(parts[1]),
                                1.0f - Float.parseFloat(parts[2])
                        );
                        break;
                    case "vn":
                        model.addNormal(
                                Float.parseFloat(parts[1]),
                                Float.parseFloat(parts[2]),
                                Float.parseFloat(parts[3])
                        );
                        break;
                    case "f": {
                        int cnt = parts.length - 1;
                        int[] vIdx = new int[cnt];
                        int[] tIdx = new int[cnt];
                        int[] nIdx = new int[cnt];
                        boolean hasT = true, hasN = true;

                        for (int i = 1; i <= cnt; i++) {
                            String[] indices = parts[i].split("/");
                            vIdx[i - 1] = Integer.parseInt(indices[0]);
                            if (indices.length > 1 && !indices[1].isEmpty()) {
                                tIdx[i - 1] = Integer.parseInt(indices[1]);
                            } else {
                                hasT = false;
                            }
                            if (indices.length > 2 && !indices[2].isEmpty()) {
                                nIdx[i - 1] = Integer.parseInt(indices[2]);
                            } else {
                                hasN = false;
                            }
                        }

                        if (cnt == 3) {
                            model.addFace(vIdx, hasT ? tIdx : null, hasN ? nIdx : null);
                        } else if (cnt == 4) {
                            model.addFace(
                                    new int[]{vIdx[0], vIdx[1], vIdx[2]},
                                    hasT ? new int[]{tIdx[0], tIdx[1], tIdx[2]} : null,
                                    hasN ? new int[]{nIdx[0], nIdx[1], nIdx[2]} : null
                            );
                            model.addFace(
                                    new int[]{vIdx[2], vIdx[3], vIdx[0]},
                                    hasT ? new int[]{tIdx[2], tIdx[3], tIdx[0]} : null,
                                    hasN ? new int[]{nIdx[2], nIdx[3], nIdx[0]} : null
                            );
                        }
                        break;
                    }
                }
            }
            reader.close();
            System.out.println("[AngelClient] Loaded OBJ model: " + path + " (" + model.getFaceCount() + " faces)");
        } catch (Exception e) {
            System.err.println("[AngelClient] Failed to parse OBJ model: " + path);
            e.printStackTrace();
        }

        cache.put(path, model);
        return model;
    }

    public static void clearCache() {
        cache.clear();
    }
}
