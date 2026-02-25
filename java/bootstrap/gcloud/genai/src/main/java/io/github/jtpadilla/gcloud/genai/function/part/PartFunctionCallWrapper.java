package io.github.jtpadilla.gcloud.genai.function.part;

import com.google.genai.types.FunctionCall;
import com.google.genai.types.Part;
import io.github.jtpadilla.gcloud.genai.function.FunctionGatewayException;

import java.util.Map;

final public class PartFunctionCallWrapper extends PartWrapper {

    final private FunctionCall functionCall;
    final private String name;
    final private Map<String, Object> args;

    public PartFunctionCallWrapper(Part part, FunctionCall functionCall) throws FunctionGatewayException {
        super(part);
        this.functionCall = functionCall;
        this.name = functionCall.name().orElseThrow(()->new FunctionGatewayException("Empty function name in part"));
        this.args = functionCall.args().orElseThrow(()->new FunctionGatewayException("Empty function args in part"));
    }

    public FunctionCall functionCall() {
        return functionCall;
    }

    public String name() {
        return name;
    }

    public Map<String, Object> args() {
        return args;
    }

    @Override
    public String toString() {
        return "PartFunctionCallWrapper{" +
                "functionCall=" + functionCall +
                '}';
    }

}
