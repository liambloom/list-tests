package io.github.liambloom.tests;

import java.util.*;
import java.util.function.*;
import java.lang.reflect.*;
import org.fusesource.jansi.AnsiConsole;
import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

public class ListTests {
    private final Random r = new Random();
    private final Runtime runtime = Runtime.getRuntime();
    private final Thread hook = new Thread(this::testCanceled);
    private final Supplier<List<Integer>> supplier;
    private long seed;
    private boolean failed = false;

    protected List<Integer> subject;
    protected List<Integer> control = new ArrayList<>();

    public ListTests(final Supplier<List<Integer>> supplier) {
        this.supplier = supplier;
        this.subject = supplier.get();
    }

    public ListTests(final List<Integer> subject) {
        this.supplier = null;
        this.subject = subject;
    }

    public class Result {
        public final List<Integer> subject = Collections.unmodifiableList(ListTests.this.subject);
        public final List<Integer> control = Collections.unmodifiableList(ListTests.this.control);
        public final long seed = ListTests.this.seed;
        public final Throwable error;
        public final Method failedOn;

        public Result() {
            this(null, null);
        }

        public Result(Method failedOn, Throwable error) {
            this.failedOn = failedOn;
            this.error = error;
        }

        public boolean succeeded() {
            return this.error == null;
        }
    }

    public Result runTests() {
        return runTests(r.nextLong());
    }

    public Result runTests(final long seed) {
        if (failed) {
            if (supplier == null)
                throw new IllegalStateException();
            else {
                this.subject = supplier.get();
                this.control = new ArrayList<>();
                failed = false;
            }
        }

        AnsiConsole.systemInstall();

        this.seed = seed;
        r.setSeed(seed);
        System.out.println("Running test " + seed);
        runtime.addShutdownHook(hook);

        for (Method method : List.class.getDeclaredMethods()) {
            System.out.print(method.getName() + "(");

            if (control.size() != subject.size())
                return endTests(new Result(method, new IllegalStateException("Sizes have become desynced (control is "
                    + control.size() + ", subject is " + subject.size() + ")")));

            if (control.size() == 0) {
                // Add test data
                int testLength = r.nextInt(10000) + 1;
                for (int j = 0; j < testLength; j++)
                    subject.add(r.nextInt());
                control.addAll(subject);
            }

            if (control.size() == 0)
                return endTests(new Result(method, new IllegalStateException("Either size() or add(Object) don't work")));

            Class<?>[] params = method.getParameterTypes();
            Object[] args = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                // int, E, Collection<?>, Collection<? extends E>, Object, Comparator<? super
                // E>, UnaryOperator<E>, T[],
                Class<?> type = params[i];
                if (i != 0)
                    System.out.print(", ");
                System.out.print(type.getSimpleName());
                if (type == int.class)
                    args[i] = r.nextInt(control.size());
                else if (type == Collection.class) {
                    int capacity = r.nextInt(control.size());
                    List<Integer> temp = new ArrayList<>(capacity);
                    for (int j = 0; j < capacity; j++)
                        temp.add(Integer.valueOf(r.nextInt()));
                    args[i] = temp;
                } else if (type == Integer.class || type == Object.class)
                    args[i] = Integer.valueOf(r.nextInt());
                else if (type == Comparator.class)
                    args[i] = Comparator.naturalOrder();
                else if (type == UnaryOperator.class)
                    args[i] = (UnaryOperator<Integer>) ((n) -> n + 1);
                else if (type == Object[].class)
                    args[i] = new Integer[r.nextInt(10000)];
            }

            System.out.print(")..");

            boolean ok;
            Throwable errMsg = null;
            try {
                Object res1 = method.invoke(control, args);

                try {
                    if (res1 instanceof Iterator) {
                        @SuppressWarnings("unchecked")
                        Iterator<Integer> iter1 = (Iterator<Integer>) res1;
                        @SuppressWarnings("unchecked")
                        Iterator<Integer> iter2 = (Iterator<Integer>) method.invoke(subject, args);
                        ok = true;
                        while (iter1.hasNext() && iter2.hasNext()) {
                            if (!iter1.next().equals(iter2.next())) {
                                ok = false;
                                errMsg = new UnequalReturnValuesException("Iterators contained differing values");
                                break;
                            }
                        }
                        if (ok == true) {
                            ok = iter1.hasNext() == iter2.hasNext();
                            errMsg = new UnequalReturnValuesException("Iterators were different lengths");
                        }
                    } 
                    else if (res1 instanceof Spliterator) {
                        // I'm just run it, to make sure it doesn't throw an error.
                        // I have no idea what a spliterator is, or how it works,
                        // so I'm not going to actually check the values, since
                        // .equals() won't work for it.
                        method.invoke(subject, args);
                        ok = true;
                    } 
                    else {
                        Object res2 = method.invoke(subject, args);
                        if (res1 == null)
                            ok = res2 == null;
                        else if (method.getReturnType().isArray())
                            ok = Arrays.equals((Object[]) res1, (Object[]) res2);
                        else
                            ok = res1.equals(res2);
                        errMsg = new UnequalReturnValuesException("Returned different values. control: %s; subject: %s",
                                res1 == null ? "null" : res1.toString(), res2 == null ? "null" : res1.toString());
                    }
                } 
                catch (Exception e) {
                    ok = false;
                    errMsg = e.getCause();
                }
            } 
            catch (Exception e1) {
                try {
                    method.invoke(control, args);
                    ok = false;
                    errMsg = e1.getCause();
                } 
                catch (Exception e2) {
                    ok = e1.getClass() == e2.getClass();
                }
            }
            final boolean sameResult = control.equals(subject);

            if (ok && sameResult)
                System.out.println(ansi().fg(GREEN).a("ok").reset());
            else {
                failed = true;
                System.out.println(ansi().fg(RED).a("fail").reset());
                System.out.printf("returned same: %b, post-values same: %b%n", ok, sameResult);
                if (ok)
                    errMsg = new IllegalStateException("control is not equal to subject");
                return endTests(new Result(method, errMsg));
            }
        }

        return endTests(new Result());
    }

    public boolean hasFailed() {
        return failed;
    }

    private Result endTests(Result result) {
        if (!result.succeeded()) {
            failed = true;
            result.error.printStackTrace();
        }
        runtime.removeShutdownHook(hook);
        AnsiConsole.systemUninstall();
        return result;
    }

    private void testCanceled() {
        System.out.println("Aborting test " + seed);
    }
}