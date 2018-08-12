package vlad.stupak.player;


import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.boontaran.games.ActorClip;
import com.boontaran.marchingSquare.MarchingSquare;

import java.util.ArrayList;

import vlad.stupak.Main;
import vlad.stupak.Setting;
import vlad.stupak.levels.Level;

public class Transport extends ActorClip{
    public  Body bodyCar, frontWheel, frontWheel2, rearWheel, rearWheel2;
    public Joint frontWheelJoint2, rearWheelJoint2;
    private boolean hasDestoyed = false;
    public boolean destroyOnNextUpdate = false;
    private boolean isTouchGround = true;
    private float jumpImpulse = Setting.JUMP_IMPULSE;
    private float jumpWait = 0;
    private static final short SPRING_CATEGORY_BITS = 1;
    private static final short WHEEL_CATEGORY_BITS = 2;

    public Body createWheel(World world, float rad, float restitution, float friction, float density) {
        BodyDef wheelDef = new BodyDef();
        wheelDef.type = BodyDef.BodyType.DynamicBody;
        wheelDef.linearDamping = 0;
        wheelDef.angularDamping = 1f;

        Body wheelBody = world.createBody(wheelDef);

        FixtureDef fDef = new FixtureDef();
        CircleShape shape = new CircleShape();
        shape.setRadius(rad);

        fDef.shape = shape;
        fDef.restitution = restitution;
        fDef.friction = friction;
        fDef.density = density;
        fDef.filter.categoryBits = WHEEL_CATEGORY_BITS;
        fDef.filter.maskBits = SPRING_CATEGORY_BITS;

        wheelBody.createFixture(fDef);
        shape.dispose();

        return wheelBody;
    }

    public Body createSpring(World world, float restitution, float friction, float density) {
        BodyDef springDef = new BodyDef();
        springDef.type = BodyDef.BodyType.DynamicBody;
        springDef.linearDamping = 0;
        springDef.angularDamping = 1f;

        Body springBody = world.createBody(springDef);

        FixtureDef fDef = new FixtureDef();
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(0.3f, 0.8f);

        fDef.shape = shape;
        fDef.restitution = restitution;
        fDef.friction = friction;
        fDef.density = density;
        fDef.isSensor = true;

        springBody.createFixture(fDef);
        shape.dispose();

        return springBody;
    }

    public float[] traceOutline(String regionName) {

        Texture bodyOutLine = Main.atlas.findRegion(regionName).getTexture();
        TextureAtlas.AtlasRegion reg = Main.atlas.findRegion(regionName);
        int w = reg.getRegionWidth();
        int h = reg.getRegionHeight();
        int x = reg.getRegionX();
        int y = reg.getRegionY();

        bodyOutLine.getTextureData().prepare();
        Pixmap allPixmap = bodyOutLine.getTextureData().consumePixmap();

        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pixmap.drawPixmap(allPixmap,0,0,x,y,w,h);

        allPixmap.dispose();

        int pixel;

        w = pixmap.getWidth();
        h = pixmap.getHeight();

        int [][] map;
        map = new int[w][h];
        for (x=0; x < w; x++) {
            for (y = 0; y < h; y++) {
                pixel = pixmap.getPixel(x, y);
                if ((pixel & 0x000000ff) == 0) {
                    map[x][y] = 0;
                } else {
                    map[x][y] = 1;
                }
            }
        }

        pixmap.dispose();

        MarchingSquare ms = new MarchingSquare(map);
        ms.invertY();
        ArrayList<float[]> traces = ms.traceMap();

        float[] polyVertices = traces.get(0);
        return polyVertices;
    }

    public Body createBodyFromTriangles(World world, Array<Polygon> triangles) {
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.DynamicBody;
        def.linearDamping = 0;
        Body body = world.createBody(def);

        for (Polygon triangle : triangles) {
            FixtureDef fDef = new FixtureDef();
            PolygonShape shape = new PolygonShape();
            shape.set(triangle.getTransformedVertices());

            fDef.shape = shape;
            fDef.restitution = 0; //0.3f эластичность
            fDef.density = 0.4f;  //1 плотность кг/m^2

            body.createFixture(fDef);
            shape.dispose();
        }
        return body;
    }

    public void onKey(boolean moveFrontKey, boolean moveBackKey) {
        float torque = Setting.WHEEL_TORQUE;
        float maxAV = Setting.SPEED;

        if (moveFrontKey) {
            if (-frontWheel.getAngularVelocity() < maxAV) {
                frontWheel.applyTorque(-torque, true);
            }
            if (-rearWheel.getAngularVelocity() < maxAV) {
                rearWheel.applyTorque(-torque, true);
            }
        }
        if (moveBackKey) {
            if (frontWheel.getAngularVelocity() < maxAV) {
                frontWheel.applyTorque(torque, true);
            }
            if (rearWheel.getAngularVelocity() < maxAV) {
                rearWheel.applyTorque(torque, true);
            }
        }
    }

    public void jumpBack(float value) {
        if (value < 0.2f) value = 0.2f;

        bodyCar.applyLinearImpulse(0, jumpImpulse * value,
                bodyCar.getWorldCenter().x + 5 / Level.WORLD_SCALE,
                bodyCar.getWorldCenter().y, true);
        isTouchGround = false;
        jumpWait = 0.3f;
    }

    public void jumpForward(float value){
        if (value < 0.2f) value = 0.2f;

        bodyCar.applyLinearImpulse(0, jumpImpulse * value,
                bodyCar.getWorldCenter().x - 4 / Level.WORLD_SCALE,
                bodyCar.getWorldCenter().y, true);
        isTouchGround = false;
        jumpWait = 0.3f;
    }

    public void touchGround() {
        isTouchGround = true;
    }

    public boolean isTouchedGround() {
        if (jumpWait > 0) return false;
        return isTouchGround;
    }

    public void destroy() {
        if (hasDestoyed) return;
        hasDestoyed = true;

        destroyOnNextUpdate = true;
    }

    public boolean isHasDestoyed() {
        return hasDestoyed;
    }
}