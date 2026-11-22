package io.github.angelclient.client.cosmetic.wavey;

import java.util.ArrayList;
import java.util.List;

public class StickSimulation3d {

    private static final float GRAVITY = 25.0F;
    private static final int ITERATIONS = 3;
    private static final float MAX_BEND = 90.0F;

    public List<Point> points = new ArrayList<>();
    private final List<Stick> sticks = new ArrayList<>();
    private Vector3 gravityDirection = new Vector3(0.0F, -1.0F, 0.0F);
    private float gravity = GRAVITY;
    private boolean sneaking;
    private boolean needsInit = true;

    public class Point implements CapePoint {
        public Vector3 position;
        public Vector3 prevPosition;

        public Point(Vector3 position) {
            this.position = position.clone();
            this.prevPosition = position.clone();
        }

        public float getLerpX(float delta) {
            return prevPosition.x + (position.x - prevPosition.x) * delta;
        }

        public float getLerpY(float delta) {
            return prevPosition.y + (position.y - prevPosition.y) * delta;
        }

        public float getLerpZ(float delta) {
            return prevPosition.z + (position.z - prevPosition.z) * delta;
        }
    }

    private static class Stick {
        Point a, b;
        float length;

        Stick(Point a, Point b, float length) {
            this.a = a; this.b = b; this.length = length;
        }
    }

    public void init(int partCount) {
        points.clear();
        sticks.clear();

        for (int i = 0; i < partCount; i++) {
            points.add(new Point(new Vector3(0, -i, 0)));
        }

        for (int i = 0; i < partCount - 1; i++) {
            sticks.add(new Stick(points.get(i), points.get(i + 1), 1.0F));
        }

        points.get(0).prevPosition = points.get(0).position.clone();
        needsInit = false;
    }

    public boolean isNeedsInit() { return needsInit; }

    public void setGravityDirection(Vector3 dir) {
        this.gravityDirection = dir;
    }

    public void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
    }

    public void applyMovement(Vector3 movement) {
        Point root = points.get(0);
        root.prevPosition.x += movement.x;
        root.prevPosition.y += movement.y;
        root.prevPosition.z += movement.z;
        root.position.x += movement.x;
        root.position.y += movement.y;
        root.position.z += movement.z;
    }

    public void simulate() {
        int partCount = points.size();

        for (int i = 1; i < partCount; i++) {
            Point p = points.get(i);
            Vector3 vel = new Vector3(
                p.position.x - p.prevPosition.x,
                p.position.y - p.prevPosition.y,
                p.position.z - p.prevPosition.z
            );
            p.prevPosition.x = p.position.x;
            p.prevPosition.y = p.position.y;
            p.prevPosition.z = p.position.z;
            p.position.x += vel.x;
            p.position.y += vel.y;
            p.position.z += vel.z;
            p.position.x += gravityDirection.x * gravity * 0.05F;
            p.position.y += gravityDirection.y * gravity * 0.05F;
            p.position.z += gravityDirection.z * gravity * 0.05F;
        }

        for (int iter = 0; iter < ITERATIONS; iter++) {
            for (Stick stick : sticks) {
                Vector3 center = new Vector3(
                    (stick.a.position.x + stick.b.position.x) / 2.0F,
                    (stick.a.position.y + stick.b.position.y) / 2.0F,
                    (stick.a.position.z + stick.b.position.z) / 2.0F
                );
                Vector3 dir = new Vector3(
                    stick.a.position.x - stick.b.position.x,
                    stick.a.position.y - stick.b.position.y,
                    stick.a.position.z - stick.b.position.z
                );
                float len = (float) Math.sqrt(dir.x * dir.x + dir.y * dir.y + dir.z * dir.z);
                if (len > 1.0E-4F) {
                    dir.x /= len; dir.y /= len; dir.z /= len;
                }
                stick.a.position.x = center.x + dir.x * stick.length / 2.0F;
                stick.a.position.y = center.y + dir.y * stick.length / 2.0F;
                stick.a.position.z = center.z + dir.z * stick.length / 2.0F;
                stick.b.position.x = center.x - dir.x * stick.length / 2.0F;
                stick.b.position.y = center.y - dir.y * stick.length / 2.0F;
                stick.b.position.z = center.z - dir.z * stick.length / 2.0F;
            }
        }

        // Hard bend prevention
        for (int i = 1; i < partCount - 1; i++) {
            Point p0 = points.get(i - 1);
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);

            float ax = p1.position.x - p0.position.x;
            float ay = p1.position.y - p0.position.y;
            float bx = p2.position.x - p1.position.x;
            float by = p2.position.y - p1.position.y;
            float dot = ax * bx + ay * by;
            float cross = ax * by - ay * bx;
            float angle = (float) Math.toDegrees(Math.atan2(cross, dot));
            angle = Math.abs(angle);

            if (angle > MAX_BEND) {
                float extra = angle - MAX_BEND;
                float midX = (p0.position.x + p2.position.x) / 2.0F;
                float midY = (p0.position.y + p2.position.y) / 2.0F;
                float dirX = p1.position.x - midX;
                float dirY = p1.position.y - midY;
                float dLen = (float) Math.sqrt(dirX * dirX + dirY * dirY);
                if (dLen > 1.0E-4F) {
                    float correction = extra * 0.05F;
                    p1.position.x += (dirX / dLen) * correction;
                    p1.position.y += (dirY / dLen) * correction;
                }
            }
        }

        // Player clipping (X axis - stay behind player)
        for (int i = 1; i < partCount; i++) {
            if (points.get(i).position.x > points.get(0).position.x) {
                points.get(i).position.x = points.get(0).position.x;
            }
        }
    }

    public boolean empty() {
        return points.isEmpty();
    }
}
