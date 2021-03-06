import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public class Main {
    public final static int A = 1;
    public final static int B = 2;
    public final static int C = 3;
    State state;

    public void main() {
        ArrayList<Set<Integer>> list = new ArrayList<>();
        state.main(list);
    }

    public void main2() throws IOException {
        OutputStream output = new ByteArrayOutputStream();
        state.main2(output);
    }

    public void setState(int state) {
        switch (state) {
            case B:
                this.state = new B();
                break;
            case A:
                this.state = new A();
                break;
            case C:
                this.state = new C();
                break;
            default:
                this.state = null;
                break;
        }
    }

    public int getState() {
        return state.getState();
    }
}