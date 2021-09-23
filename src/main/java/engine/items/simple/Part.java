package engine.items.simple;

public class Part {
    private TypeTopology type;
    private int startX;
    private int count;

    public Part(TypeTopology type, int startX, int count) {
        this.type = type;
        this.startX = startX;
        this.count = count;
    }

    public TypeTopology getType() {
        return type;
    }

    public void setType(TypeTopology type) {
        this.type = type;
    }

    public int getStartX() {
        return startX;
    }

    public void setStartX(int startX) {
        this.startX = startX;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
