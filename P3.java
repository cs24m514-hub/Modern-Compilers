import syntaxtree.*;
import visitor.*;

public class P3 {
    public static void main(String[] args) {
        try {
            TACoJavaParser parser = new TACoJavaParser(System.in);
            Node root = parser.Goal();

	    OptimizeVisitor opt = new OptimizeVisitor();
            root.accept(opt);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}