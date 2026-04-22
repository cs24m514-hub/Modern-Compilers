import visitor.*;
import syntaxtree.*;
import java.util.*;

public class CCPVisitor extends DepthFirstVisitor {

    Map<String, LatticeValue> env = new HashMap<>();

private LatticeValue eval(Object obj) {
    if (obj instanceof IntegerLiteral) {
        return new LatticeValue(
            Integer.parseInt(((IntegerLiteral)obj).f0.tokenImage));
    }
    if (obj instanceof Identifier) {
        String n = ((Identifier)obj).f0.tokenImage;
        return env.getOrDefault(n, LatticeValue.NAC());
    }
    return LatticeValue.NAC();
}

    public void visit(AssignmentStatement n) {
    String var = n.f0.f0.tokenImage;
    Object rhs = n.f2.f0.choice;

    if (rhs instanceof IntegerLiteral) {
        IntegerLiteral i = (IntegerLiteral) rhs;
        env.put(var, new LatticeValue(Integer.parseInt(i.f0.tokenImage)));
    }
    else if (rhs instanceof Identifier) {
        String name = ((Identifier) rhs).f0.tokenImage;
        env.put(var, env.getOrDefault(name, LatticeValue.NAC()));
    }
    else if (rhs instanceof TimesExpression) {
        TimesExpression t = (TimesExpression) rhs;
        LatticeValue l = eval(t.f0);
        LatticeValue r = eval(t.f2);

        if (l.type == LatticeValue.Type.CONST && r.type == LatticeValue.Type.CONST)
            env.put(var, new LatticeValue(l.value * r.value));
        else
            env.put(var, LatticeValue.NAC());
    }
    else if (rhs instanceof CompareExpression) {
        CompareExpression c = (CompareExpression) rhs;
        LatticeValue l = eval(c.f0);
        LatticeValue r = eval(c.f2);

        if (l.type == LatticeValue.Type.CONST && r.type == LatticeValue.Type.CONST)
            env.put(var, new LatticeValue(l.value < r.value ? 1 : 0));
        else
            env.put(var, LatticeValue.NAC());
    }
    else {
        env.put(var, LatticeValue.NAC());
    }

    super.visit(n);
}


}