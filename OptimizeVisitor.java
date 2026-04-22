import visitor.*;
import syntaxtree.*;
import java.util.*;

public class OptimizeVisitor extends DepthFirstVisitor {

    Map<String, Integer> env;
    Map<String, MethodDeclaration> methodMap = new HashMap<>();

    // ================= GOAL =================
    public void visit(Goal n) {

        // Collect all methods first
        if (n.f1.present()) {
            for (Node td : n.f1.nodes) {
                TypeDeclaration t = (TypeDeclaration) td;

                if (t.f0.choice instanceof ClassDeclaration) {
                    ClassDeclaration cd = (ClassDeclaration) t.f0.choice;
                    collectMethods(cd.f4);
                }
            }
        }

        // Print program
        n.f0.accept(this);

        if (n.f1.present()) {
            for (Node td : n.f1.nodes) {
                td.accept(this);
            }
        }
    }

    private void collectMethods(NodeListOptional list) {
        if (!list.present()) return;

        for (Node node : list.nodes) {
            MethodDeclaration md = (MethodDeclaration) node;
            methodMap.put(md.f2.f0.tokenImage, md);
        }
    }

    // ================= MAIN =================
    public void visit(MainClass n) {

        env = new HashMap<>();

        System.out.println("class " + n.f1.f0.tokenImage + " {");
        System.out.println("    public static void main(String[] " + n.f11.f0.tokenImage + ") {");

        n.f14.accept(this);
        n.f15.accept(this);

        System.out.println("    }");
        System.out.println("}");
    }

    // ================= CLASS =================
    public void visit(ClassDeclaration n) {
        System.out.println("class " + n.f1.f0.tokenImage + "{");
        n.f4.accept(this);
        System.out.print("}");
    }

    // ================= METHOD =================
    public void visit(MethodDeclaration n) {

        env = new HashMap<>();

        System.out.print("    public ");
        n.f1.accept(this);
        System.out.print(" " + n.f2.f0.tokenImage + "(");

        if (n.f4.present()) n.f4.node.accept(this);

        System.out.println(") {");

        n.f7.accept(this);
        n.f8.accept(this);

        int returnVal = evalExpression(n.f10);

        System.out.println("        return " + returnVal + ";");
        System.out.println("    }");
    }

    // ================= VAR =================
    public void visit(VarDeclaration n) {
        System.out.print("        ");
        n.f0.accept(this);
        System.out.println(" " + n.f1.f0.tokenImage + ";");
    }

    public void visit(IntegerType n) { System.out.print("int"); }
    public void visit(BooleanType n) { System.out.print("boolean"); }

    // ================= ASSIGN =================
    public void visit(AssignmentStatement n) {

        String var = n.f0.f0.tokenImage;

        if (n.f2.f0.choice instanceof MessageSend) {

            MessageSend m = (MessageSend) n.f2.f0.choice;

            List<Integer> args = getArgs(m);

            int result = evaluateMethod(m.f2.f0.tokenImage, args);

            env.put(var, result);

            printCall(m);

            return;
        }

        int val = evalExpression(n.f2);
        env.put(var, val);
    }

    // ================= IF =================
    public void visit(IfStatement n) {

        int cond = evalExpression(n.f2);

        if (cond != 0)
            n.f4.accept(this);
        else
            n.f6.accept(this);
    }

    // ================= PRINT =================
    public void visit(PrintStatement n) {

        int val = evalExpression(n.f2);

        System.out.println("        System.out.println(" + val + ");");
    }

    // ================= EXPRESSION =================
    private int evalExpression(Node n) {

        if (n instanceof Identifier) {
            return env.getOrDefault(((Identifier) n).f0.tokenImage, 0);
        }

        if (n instanceof Expression) {
            return evalExpression(((Expression) n).f0.choice);
        }

        if (n instanceof PrimaryExpression) {
            PrimaryExpression p = (PrimaryExpression) n;

            if (p.f0.choice instanceof IntegerLiteral)
                return Integer.parseInt(((IntegerLiteral)p.f0.choice).f0.tokenImage);

            if (p.f0.choice instanceof Identifier)
                return env.getOrDefault(((Identifier)p.f0.choice).f0.tokenImage, 0);
        }

        if (n instanceof PlusExpression) {
            PlusExpression p = (PlusExpression) n;
            return evalExpression(p.f0) + evalExpression(p.f2);
        }

        if (n instanceof TimesExpression) {
            TimesExpression t = (TimesExpression) n;
            return evalExpression(t.f0) * evalExpression(t.f2);
        }

        if (n instanceof CompareExpression) {
            CompareExpression c = (CompareExpression) n;
            return (evalExpression(c.f0) < evalExpression(c.f2)) ? 1 : 0;
        }

        if (n instanceof MessageSend) {
            MessageSend m = (MessageSend) n;
            return evaluateMethod(m.f2.f0.tokenImage, getArgs(m));
        }

        return 0;
    }

    // ================= METHOD EXECUTION =================
    private int evaluateMethod(String name, List<Integer> args) {

        MethodDeclaration md = methodMap.get(name);

        Map<String, Integer> localEnv = new HashMap<>();

        // Bind parameters
        if (md.f4.present()) {
            FormalParameterList params = (FormalParameterList) md.f4.node;

            localEnv.put(params.f0.f1.f0.tokenImage, args.get(0));

            int i = 1;
            for (Node node : params.f1.nodes) {
                FormalParameterRest rest = (FormalParameterRest) node;
                localEnv.put(rest.f1.f1.f0.tokenImage, args.get(i++));
            }
        }

        // Evaluate method body
        return executeMethod(md, localEnv);
    }

    private int executeMethod(MethodDeclaration md, Map<String, Integer> localEnv) {

        Map<String, Integer> old = env;
        env = localEnv;

        if (md.f8.present()) {
            for (Node s : md.f8.nodes) {
                s.accept(this);
            }
        }

        int val = evalExpression(md.f10);

        env = old;
        return val;
    }

    // ================= HELPERS =================
    private List<Integer> getArgs(MessageSend m) {

        List<Integer> list = new ArrayList<>();

        if (!m.f4.present()) return list;

        ArgList al = (ArgList) m.f4.node;

        list.add(evalExpression(al.f0));

        for (Node node : al.f1.nodes) {
            ArgRest ar = (ArgRest) node;
            list.add(evalExpression(ar.f1));
        }

        return list;
    }

    private void printCall(MessageSend n) {

        System.out.print("        ");

        PrimaryExpression p = (PrimaryExpression) n.f0;

        if (p.f0.choice instanceof Identifier)
            System.out.print(((Identifier)p.f0.choice).f0.tokenImage);
        else
            System.out.print("this");

        System.out.print("." + n.f2.f0.tokenImage + "(");

        List<Integer> args = getArgs(n);

        for (int i = 0; i < args.size(); i++) {
            if (i > 0) System.out.print(", ");
            System.out.print(args.get(i));
        }

        System.out.println(");");
    }

    // ================= DEFAULT =================
    public void visit(NodeListOptional n) {
        if (n.present()) {
            for (Node node : n.nodes) node.accept(this);
        }
    }

    public void visit(Statement n) { n.f0.accept(this); }

    public void visit(FormalParameterList n) {
        n.f0.accept(this);
        for (Node node : n.f1.nodes) node.accept(this);
    }

    public void visit(FormalParameter n) {
        n.f0.accept(this);
        System.out.print(" " + n.f1.f0.tokenImage);
    }

    public void visit(FormalParameterRest n) {
        System.out.print(", ");
        n.f1.accept(this);
    }

    public void visit(Identifier n) {
        System.out.print(n.f0.tokenImage);
    }
}