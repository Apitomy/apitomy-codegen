
package org.example.api.beans;

import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "widgets",
    "labels"
})
@Generated("jsonschema2pojo")
public class Inventory {

    @JsonProperty("widgets")
    private Map<String, List<Widget>> widgets;
    @JsonProperty("labels")
    private Map<String, String> labels;

    @JsonProperty("widgets")
    public Map<String, List<Widget>> getWidgets() {
        return widgets;
    }

    @JsonProperty("widgets")
    public void setWidgets(Map<String, List<Widget>> widgets) {
        this.widgets = widgets;
    }

    @JsonProperty("labels")
    public Map<String, String> getLabels() {
        return labels;
    }

    @JsonProperty("labels")
    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

}
