package eu.printingin3d.javascad.vrl;

public class VertexHolder {

    private final float[] vertex;
    private final float[] normals;
    private final int verticesCount;

    public VertexHolder(float[] vertex, float[] normals, int verticesCount) {
        this.vertex = vertex;
        this.normals = normals;
        this.verticesCount = verticesCount;
    }

    public float[] getVertex() {
        return vertex;
    }

    public float[] getNormals() {
        return normals;
    }

    public int getVerticesCount() {
        return verticesCount;
    }
}
