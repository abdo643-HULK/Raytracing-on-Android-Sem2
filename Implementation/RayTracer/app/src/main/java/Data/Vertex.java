package Data;

import Util.Geometry.Vector;

public class Vertex {

    private static final short NO_INDEX = -1;

    private Vector position;
    private short textureIndex = NO_INDEX;
    private short normalIndex = NO_INDEX;
    private Vertex duplicateVertex = null;
    private short index;

    public Vertex(short index, Vector position){
        this.index = index;
        this.position = position;
    }

    public short getIndex(){
        return index;
    }

    public boolean isSet(){
        return textureIndex!=NO_INDEX && normalIndex!=NO_INDEX;
    }

    public boolean hasSameTextureAndNormal(int textureIndexOther,int normalIndexOther){
        return textureIndexOther==textureIndex && normalIndexOther==normalIndex;
    }

    public void setTextureIndex(short textureIndex){
        this.textureIndex = textureIndex;
    }

    public void setNormalIndex(short normalIndex){
        this.normalIndex = normalIndex;
    }

    public Vector getPosition() {
        return position;
    }

    public short getTextureIndex() {
        return textureIndex;
    }

    public short getNormalIndex() {
        return normalIndex;
    }

    public Vertex getDuplicateVertex() {
        return duplicateVertex;
    }

    public void setDuplicateVertex(Vertex duplicateVertex) {
        this.duplicateVertex = duplicateVertex;
    }

}