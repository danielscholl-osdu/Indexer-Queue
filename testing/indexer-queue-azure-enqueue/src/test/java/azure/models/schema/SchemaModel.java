package azure.models.schema;

import lombok.Getter;
import lombok.Setter;

import java.util.StringJoiner;

@Getter
@Setter
public class SchemaModel {

    private SchemaInfo schemaInfo;
    private Object schema;

    @Override
    public String toString() {
        return new StringJoiner(", ", SchemaModel.class.getSimpleName() + "[", "]")
            .add("schemaInfo=" + schemaInfo)
            .add("schema=" + schema)
            .toString();
    }
}
