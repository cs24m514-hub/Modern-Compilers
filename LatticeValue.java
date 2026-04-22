class LatticeValue {
    enum Type { CONST, NAC }

    Type type;
    Integer value;

    public LatticeValue(int v) {
        type = Type.CONST;
        value = v;
    }

    public LatticeValue(Type t) {
        type = t;
        value = null;
    }

    public static LatticeValue NAC() {
        return new LatticeValue(Type.NAC);
    }

    @Override
    public String toString() {
        if (type == Type.CONST) return value.toString();
        return "NAC";
    }
}