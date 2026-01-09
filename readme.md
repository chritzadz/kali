So, this is my take on Crafting Interpreters. By no means am I trying to create a new programming language that is useful, optimized, or better than any existing languages, but rather use this as a foundation for my understanding how a language is interpreted. Thanks to Robert Nystrom for the excellent book and explanations on crafting an interpreter, and for providing the backbone of the Kali language.

With that being said, the language is interpreted in Java, although Kali will probably be more high-level than Java. All of the implementation here is to combine the features I like about a language and try to dismiss, as much as possible, the features I do not like - I will try to implement datatypes to prevent dynamically-typed variables and try to remove the 'truthy' and 'falsey' naure of certain comparisons :)

Kali is actually taken from one of the 5 largest islands in Indonesia - Kalimantan, also known as Borneo. My simple reason for choosing Kali(mantan) is because it is planned that the capital of Indonesia will be moved to Borneo. I hope you guys get my reasoning here. It is stupid, honestly.


To run the code, use the following command:
javac kali/Scanner.java && java kali.Kali <path-file>
javac kali/Scanner.java && java kali.Kali test.txt

AST generation
java tool/GenerateAst.java kali