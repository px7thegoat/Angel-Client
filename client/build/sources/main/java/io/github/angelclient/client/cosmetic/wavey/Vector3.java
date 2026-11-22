package io.github.angelclient.client.cosmetic.wavey;

public class Vector3 {

    public float x;
    public float y;
    public float z;

    public Vector3() {}

    public Vector3(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
    }

    public Vector3 clone() {
        return new Vector3(x, y, z);
    }

    public Vector3 copy(Vector3 v) {
        this.x = v.x; this.y = v.y; this.z = v.z;
        return this;
    }

    public Vector3 add(Vector3 v) {
        this.x += v.x; this.y += v.y; this.z += v.z;
        return this;
    }

    public Vector3 add(float x, float y, float z) {
        this.x += x; this.y += y; this.z += z;
        return this;
    }

    public Vector3 subtract(Vector3 v) {
        this.x -= v.x; this.y -= v.y; this.z -= v.z;
        return this;
    }

    public Vector3 div(float f) {
        this.x /= f; this.y /= f; this.z /= f;
        return this;
    }

    public Vector3 mul(float f) {
        this.x *= f; this.y *= f; this.z *= f;
        return this;
    }

    public Vector3 cross(Vector3 v) {
        float cx = this.y * v.z - this.z * v.y;
        float cy = this.z * v.x - this.x * v.z;
        float cz = this.x * v.y - this.y * v.x;
        this.x = cx; this.y = cy; this.z = cz;
        return this;
    }

    public Vector3 normalize() {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len > 1.0E-4F) {
            this.x /= len; this.y /= len; this.z /= len;
        }
        return this;
    }

    public float sqrMagnitude() {
        return x * x + y * y + z * z;
    }
}
