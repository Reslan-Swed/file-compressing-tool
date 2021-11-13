package utils;

public enum TAGS {
    HEAD("HEAD"), BODY("BODY"), DICTIONARY("DICTIONARY"), DIRECTORY("DIRECTORY"), FILE("FILE");
    private String value;

    TAGS(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
