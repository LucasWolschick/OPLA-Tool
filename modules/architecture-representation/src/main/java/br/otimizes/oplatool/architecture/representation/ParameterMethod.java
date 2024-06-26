package br.otimizes.oplatool.architecture.representation;


/**
 * Methods representation class
 *
 * @author edipofederle<edipofederle @ gmail.com>
 */
public class ParameterMethod {

    private String name;
    private String type;
    private String direction;

    public ParameterMethod(String name, String type, String direction) {
        this.name = name;
        this.type = type;
        this.direction = direction;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ParameterMethod other = (ParameterMethod) obj;
        if (name == null) {
            return other.name == null;
        } else return name.equals(other.name);
    }
}
