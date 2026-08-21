---
layout: posts
title: Introduction to Algorithms, Programming, and Compilers
permalink: /csa/1.1
author: Zhengji Li
---

# Hack #1

### **Activity**: Setting up a new phone

1. Buy a phone thorugh online purposes or a physical store
2. Wait for phone to ship if bought online
3. While waiting buy a phone case and screen protector online
3. Unbox phone by opening the box
4. Plug phone in via respective charger, usually usb-c
5. Boot phone by holding the power button
6. Follow instructions on phone to create account & sign in
7. Download apps of you're own discretion
8. Put on phone case and screen protector by following instructions
9. Customize phone to you're own discretion
10. You now have working phone

# Hack 2: Identify the Bug

### **Algorithm**: Send an Email

1. Open email application
2. Log into your account
3. Enter recipient's email address
4. Write subject line
5. Type the message
6. Click "Send"

# Hack 3: Code the Algorithim

### **Algorithim**: Grade Calculator


```python
def calculate_grade(score1, score2, score3):
    """
    Calculate letter grade from three test scores
    
    Args:
        score1, score2, score3: Test scores (integers)
    
    Returns:
        grade: Letter grade (string)
    """
    total = score1 + score2 + score3

    average = total / 3

    if average >= 90:
        return 'A'

    elif average >= 80:
        return 'B'

    elif average >= 70:
        return 'C'

    elif average >= 60:
        return 'D'

    else:
        return 'F'
    

# Test your function!
print("Test 1:", calculate_grade(95, 92, 88))  # Should be 'A'
print("Test 2:", calculate_grade(85, 80, 82))  # Should be 'B'
print("Test 3:", calculate_grade(75, 70, 72))  # Should be 'C'
print("Test 4:", calculate_grade(65, 60, 62))  # Should be 'D'
print("Test 5:", calculate_grade(55, 50, 52))  # Should be 'F'
```

    Test 1: A
    Test 2: B
    Test 3: C
    Test 4: D
    Test 5: F
    
