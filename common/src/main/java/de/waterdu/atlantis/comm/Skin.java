package de.waterdu.atlantis.comm;

/**
 * Holds the Base64 texture value of a player skin and its signature.
 */
public class Skin {

    private String skin;
    private String signature;

    /**
     * Creates an empty skin with no texture and no signature.
     */
    public Skin() {
        this("", "");
    }

    /**
     * Creates a skin with a texture and a signature.
     *
     * @param skin the Base64 texture value, or an empty string for none
     * @param signature the signature of the texture, or an empty string for none
     */
    public Skin(String skin, String signature) {
        this.skin = skin;
        this.signature = signature;
    }

    /**
     * Checks if this skin has a texture value.
     *
     * @return {@code true} if the texture value is not empty
     */
    public boolean hasSkin() {
        return !this.skin.isEmpty();
    }

    /**
     * Checks if this skin has a signature.
     *
     * @return {@code true} if the signature is not empty
     */
    public boolean hasSignature() {
        return !this.signature.isEmpty();
    }

    /**
     * @return the Base64 texture value, or an empty string for none
     */
    public String skin() {
        return this.skin;
    }

    /**
     * Sets the texture value.
     *
     * @param skin the Base64 texture value, or an empty string for none
     */
    public void setSkin(String skin) {
        this.skin = skin;
    }

    /**
     * @return the signature of the texture, or an empty string for none
     */
    public String signature() {
        return this.signature;
    }

    /**
     * Sets the signature of the texture.
     *
     * @param signature the signature, or an empty string for none
     */
    public void setSignature(String signature) {
        this.signature = signature;
    }
}
