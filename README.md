# List Tests

This is a simple library meant for testing an implementation of the java `List` interface. A simple usage would be:

```java
import io.github.liambloom.tests.ListTests;
...
new ListTests(new MyCustomListImplementation<Integer>()).runTests();
```

### A few things to note:
- This only supports lists of *integers*. It does not support lists of any other type.
- The test data is randomized, so you may want to call the `runTests()` method more than once.
- If you want to use a specific seed to generate the test data, you can pass it into `runTests(long)`
- If you call `runTests()` multiple times on the same `ListTests` object, the state of the list *will be retained between tests*. This means that if you run the tests multiple times with the same seed, you are not guaranteed to get the same output.

Given the fact that the test data is randomized, you may want to call `runTests()` more than once:
```java
import io.github.liambloom.tests.ListTests;
...
ListTests tests = new ListTests(new MyCustomListImplementation<Integer>());
for (int i = 0; i < 20; i++) // Feel free to run it any number of times. I will usually do 100, but that is probably excessive.
  tests.runTests();
```


### How does it work?

Reflection is used to iterate over and call every method in the `List` interface. This method is called on both your custom list *and* an `ArrayList`. After each method call, the outputs and the state of the array are compared. A test fails if they are not equal.

### Acknowledgements

- [fusecore/jansi](https://github.com/fusesource/jansi)

### License

This project is licensed under [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0). More details can be found in [`LICENSE`](/LICENSE). 

&copy; 2020 Liam Bloom
