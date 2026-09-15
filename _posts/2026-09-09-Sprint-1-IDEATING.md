---
title: ideating/documentation
layout: post
description: ideas for everyhting sprint 1 related
permalink: /spt1
author: Jade
showReadingTime: true
---



By the end of this assignment, you will:

 - Master POJO Design — Understand how Plain Old Java Objects form the foundation of both the AP CSA exam and modern frameworks like Spring
 - Implement Data Persistence — Use JPA and SQLite to reliably store and retrieve your data object
 - Build a REST API — Create Spring Boot endpoints that expose your data with proper HTTP methods (GET, POST, PUT, DELETE)
 - Design Full-Stack Architecture — Connect a backend API to a frontend (GitHub Pages OR Thymeleaf admin page)
 - Portfolio & Exam Preparation — Document your work in a blog that demonstrates OOP mastery and prepares you for AP CSA exam

# DATA OBJECT DOMAIN MODEL
what is my team per se <br/>
my team is like<br/>
backend<br/>
we have admin panel<br/>
what coudl be useful?<br/>

i find things useful

i find that simple infographics and ui screen useful

i make

# a dedicated chart with graphics of different users, their schools, their github and student ids, as well as name and join date. this chart will differentiate between students as well as guest accounts added through the mentors

therefore i choose option b

Design your POJO based on your data object

    Identify all required fields (e.g., id, name, description, createdAt, updatedBy)
    Choose appropriate data types (primitives, Strings, LocalDateTime, etc.)
    Consider relationships (e.g., a Project belongs to a Team)


required fields:<br/>
id<br/>
name<br/>
createdAt<br/>
email<br/>

data types:<br/>
localdatetime<br/>
strings<br/>

relationships:<br/>
my project should tie into the admin panel and be a useful tool for administrators when dealing with account problems/reset passwords and similar.

Lombok Annotations:

https://dev.to/gianfcop98/10-lombok-annotations-every-java-developer-should-know-pcd

review review review

https://medium.com/javarevisited/all-the-16-lombok-annotations-explained-in-a-4-minute-article-926f71934ec6


this is my pojo
```java
import jakarta.persistence.*
import lombok.*

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class POJO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String school;
    private String studentID;
    private String githubUsername;
    private String accountType;
    private LocalDateTime createdAt;

}
```

Lombok generates the repeitive code, and the annotations automatically generate the missing code when compiling.

The `@Entity` annotation is part of the JPA library, it bridges the gap between java code and the database 

The `@Id` annotation is part of the JPA library, it specifies the primary key of an entity -- this allows for multipule users of the same name

The `@GeneratedValue` annnotation is used with the `@Id` annotation, and is used to generate primary values for an entity

The `@NoArgsConstructor` creates a constructor with no paramaters, however if not possible then compiler error. On the other hand, `@AllArgsConstructor` ccreates a constructor with one parameter for each field in the class.

The `@Getter` and `@Setter` annotations are used to generate the default getter/setter, without needing to write them down

A getter allows another part of the program to retrieve a private instance variable. <br/>
For example in the following code:

```java
public String getName() {
    return name;
}
```

I can then write:

```java
Admin user = new Admin();

user.setName("Ish Anjha");

System.out.println(user.getName());
```

The output will be:
```
Ish Anjha
 ```

A setter modifies an object's instance variable. <br/>
For example in the following code:

```java
public void setEmail(stringEmail) {
    this.email = email;
}
```

Interjecting the 
```java
user.setEmail("ishanjha100@gmail.com")
```

The state of the `user` object has now changed
The keyword `this` refers to the object

```java
this.email = email;
```

In this example:
 - `this.email = email;` is the instance variable
 - `email` is the parameter in the method



# Connection to APCSA

Even if it is used in professional enviornments, **Lombok** is not used in the APCSA exam <br/>
Therefore, I need to learn how to write the methods Lombok generates by myself.

### Instance Variables
My POJO contains these variables <br/>
`private String name;` <br/>
`private String email;` <br/>

These variables have the same class design used in APCSA <br/>
Using the `private` variable helps show encapsulation, which is one of the core OOP pillars of java

### Constructors

In CSA, code I normally write code such as this
```java
public AdminUser(String name, String email) {
    this.name = name;
    this.email = email;
}
```

Lombok automatically generates these annotations, 
however I still need to know what a constructor is

A constructor:
 - Creates the initial state of an object
 - recives values through parameters
 - assigns those values to instance variables

### Accessor Methods

In CSA, getters are known as acessor methods, as they provide access to a state of object, without changing it.

For example, 
```java
public String getName(){
    return name;
}
```
`@Getter` performs the same task

### Mutator Methods

Setters are known as mutator methods as they "mutate" (modify) the state of an object

Example:
```java
public void setName(String name) {
    this.name = name;
}
```

Lombok generates this through `@Setter`

## Lombok vs CSA

basically its like this:

csa:
I learn how and why these methods work <br/>
                 | <br/>
V <br/>
Lombok&Spring boot: <br/>
Getter<br/>
Setter<br/>
Constructor<br/>

#### TL;DR
Lombok removes repetitive code, **after** I know how it works

# My Code
![POJO Code](POJO.png)

![bytecode](bytecode.png)
