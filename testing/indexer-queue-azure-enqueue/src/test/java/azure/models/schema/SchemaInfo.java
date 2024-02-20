package azure.models.schema;

import lombok.Getter;
import lombok.Setter;

import java.util.StringJoiner;

@Getter
@Setter
public class SchemaInfo {

    private SchemaIdentity schemaIdentity;
    private SchemaStatus status;

    @Override
    public String toString() {
        return new StringJoiner(", ", SchemaInfo.class.getSimpleName() + "[", "]")
            .add("schemaIdentity=" + schemaIdentity)
            .add("status=" + status)
            .toString();
    }

    public enum SchemaStatus {
        PUBLISHED, OBSOLETE, DEVELOPMENT
    }
}
