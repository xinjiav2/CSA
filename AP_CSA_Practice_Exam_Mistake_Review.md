---
title: College Board Review
permalink: /mcqreview/
author: Jade
---

# AP Computer Science A — Practice Exam Mistake Review

This guide reviews the five missed questions (Q7, Q9, Q19, Q35, and Q41), explains the correct answers, and identifies lessons to revisit.

## Quick reference

| Question | My answer | Correct answer | Lessons to review |
|---|---|---|---|
| Q7 | A | **B** | **1.15 String Manipulation** |
| Q9 | D | **C** | **3.08 Scope and Access** |
| Q19 | D | **C** | **1.11 Math Class**, 1.05 Casting and Range of Variables |
| Q35 | C | **D** | **4.09 Traversing ArrayLists**, 4.10 Implementing ArrayList Algorithms |
| Q41 | C (4) | **D (5)** | **2.11 Nested Iteration**, 4.04 Array Traversals, 4.05 Implementing Array Algorithms |

---

## Q7 — Print `HIDE` using substrings

**My answer: A**
```java
original.substring(4) + original.substring(0, 1)
```

**Correct answer: B**
```java
original.substring(4) + original.substring(0, 2)
```

**Original code:**
```java
String original = "DEFGHI";
String result = /* missing code */;
System.out.println(result);
```

### What I got wrong
I used `substring(0, 1)`, which only includes the character at index `0` (`D`). That creates `"HID"`, not `"HIDE"`.

### Why B is correct
Java string indices are zero-based:

| Index | 0 | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|---|
| Character | D | E | F | G | H | I |

- `original.substring(4)` returns `"HI"` (from index 4 through the end).
- `original.substring(0, 2)` returns `"DE"` (indices 0 and 1; **end index 2 is excluded**).
- Concatenating them produces `"HIDE"`.

**Remember:** `substring(start, end)` includes `start` but excludes `end`.

**Review:** 1.15 String Manipulation.

---

## Q9 — `Measurement` class compilation error

**My answer: D:** The `feet` and `inches` variables cannot be accessed from `toInches`.

**Correct answer: C:** The `toInches` method cannot be accessed from a class other than `Measurement`.

**Relevant code:**
```java
public class Measurement {
    private int feet;
    private int inches;

    public Measurement(int numFeet, int numInches) {
        feet = numFeet;
        inches = numInches;
    }

    private int toInches() {
        return feet * 12 + inches;
    }
}

public class Calculations {
    public static boolean compare(Measurement m1, Measurement m2) {
        if (m1.toInches() >= m2.toInches()) {
            return true;
        }
        return false;
    }
}
```

### What I got wrong
I assumed `private` fields could not be accessed by a method in the same class. In fact, **all methods of `Measurement` can access its private fields**, including `toInches()`.

### Why C is correct
`toInches()` is declared `private` in `Measurement`. The separate `Calculations` class tries to call it through `m1.toInches()` and `m2.toInches()`, which is not allowed. One possible fix is to declare `toInches()` as `public`.

**Remember:** `private` limits access to the declaring class, not to a single method. A `public` method can be called from other classes when otherwise accessible.

**Review:** 3.08 Scope and Access.

---

## Q19 — Random integer range

**My answer: D:** The expression generates integers from 10 through 16, inclusive.

**Correct answer: C:** The expression generates integers from **10 through 15, inclusive**.

**Code:**
```java
int r = (int) (Math.random() * 6) + 10;
```

### What I got wrong
I counted 16 as a possible result. But `Math.random()` returns a `double` from **0.0 inclusive to 1.0 exclusive**, so the product can never reach 6.

### Why C is correct
1. `Math.random()` is in `[0.0, 1.0)`.
2. Multiplying by `6` gives `[0.0, 6.0)`.
3. Casting to `int` truncates the nonnegative decimal, giving `0, 1, 2, 3, 4, 5`.
4. Adding `10` gives **10, 11, 12, 13, 14, 15**.

To generate 10 through 16 inclusive instead, use:
```java
int r = (int) (Math.random() * 7) + 10;
```

**Remember:** `(int) (Math.random() * n) + min` generates integers from `min` through `min + n - 1`.

**Review:** 1.11 Math Class; 1.05 Casting and Range of Variables.

---

## Q35 — Determine what an `ArrayList` traversal checks

**My answer: C:** Returns `true` if the numbers are sorted least to greatest.

**Correct answer: D:** Returns `true` if the numbers are sorted **greatest to least**, and `false` otherwise.

**Code:**
```java
public static boolean mystery(ArrayList<Integer> numbers) {
    int prev = Integer.MAX_VALUE;
    for (int num : numbers) {
        if (num > prev) {
            return false;
        }
        prev = num;
    }
    return true;
}
```

### What I got wrong
I interpreted the comparison as checking for ascending order. The condition `num > prev` actually **rejects increases**: as soon as the current value exceeds the previous value, the method returns `false`.

### Why D is correct
Trace `numbers = [9, 7, 7, 3]`:

| `num` | `prev` before check | Is `num > prev`? | Outcome |
|---|---|---|---|
| 9 | `Integer.MAX_VALUE` | No | Continue; `prev = 9` |
| 7 | 9 | No | Continue; `prev = 7` |
| 7 | 7 | No | Continue; `prev = 7` |
| 3 | 7 | No | Continue; `prev = 3` |

The loop finishes and returns `true`. If the list were `[3, 7, 9]`, it would return `false` when it encounters `7 > 3`.

**Important nuance:** Equal neighboring values are allowed, so the precise property is **non-increasing order** (descending with ties permitted). An empty list also returns `true`.

**Remember:** When tracing a condition inside a loop, ask **what causes an early return**, not just what the comparison looks like.

**Review:** 4.09 Traversing ArrayLists; 4.10 Implementing ArrayList Algorithms.

---

## Q41 — Nested loops comparing color-name lengths

**My answer: C (`4`)**

**Correct answer: D (`5`)**

**Code:**
```java
String[] colors = {"red", "green", "orange", "black", "yellow", "brown"};
int result = -1;
for (int j = 0; j < colors.length; j++) {
    for (int k = j + 1; k < colors.length; k++) {
        if (colors[j].length() == colors[k].length()) {
            result = k;
        }
    }
}
```

### What I got wrong
I selected index `4`, but the code **does not stop after finding a match**. Every subsequent matching pair overwrites `result` with its later index `k`.

### Why D is correct

| Index | Color | Length |
|---|---|---|
| 0 | red | 3 |
| 1 | green | 5 |
| 2 | orange | 6 |
| 3 | black | 5 |
| 4 | yellow | 6 |
| 5 | brown | 5 |

Matches are encountered in this order:

| `j` | `k` | Matching words | `result` becomes |
|---|---|---|---|
| 1 | 3 | green, black | 3 |
| 1 | 5 | green, brown | 5 |
| 2 | 4 | orange, yellow | 4 |
| 3 | 5 | black, brown | **5** |

The **last** matching pair is `black` and `brown`, so the final value of `result` is **5**.

**Remember:** In nested-loop tracing, follow the exact execution order and update the variable every time the assignment executes. The last assignment wins unless there is a `break`, `return`, or other control-flow change.

**Review:** 2.11 Nested Iteration; 4.04 Array Traversals; 4.05 Implementing Array Algorithms.

---

## Study priorities

- [ ] **1.15 String Manipulation:** Practice `substring` boundaries and zero-based indexing.
- [ ] **3.08 Scope and Access:** Distinguish access within a class from access in a different class.
- [ ] **1.11 Math Class / 1.05 Casting:** Practice inclusive and exclusive random-number bounds.
- [ ] **4.09 Traversing ArrayLists:** Trace comparisons, updates, and early returns.
- [ ] **2.11 Nested Iteration / 4.04 Array Traversals:** Trace both loop indices and every assignment.
- [ ] **4.05 / 4.10 Array and ArrayList Algorithms:** Practice interpreting what loops accomplish.

**Overall pattern:** Q7, Q9, and Q19 were specific Java API/access-rule misunderstandings. Q35 and Q41 were loop-tracing mistakes. Prioritize tracing loop conditions and updates, then review the precise rules for strings, visibility, and random ranges.
