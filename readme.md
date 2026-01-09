So, this is my take on Crafting Interpreter. By no means I am trying to create a new programming language that is useful, optimize, or be better than all of the existing language, but rather use this as a foundation of understanding how language in interpreted until it is executable. Thanks to Robert Nystrom for the excellent book and also explanation in crafting an interpreter, and also provide the backbone of the Kali language.

With that being said, the language is interpreted to Java, although, Kali will probably will be more high-level than Java. All of the implementation here is to combine the features i like about a language and try to dismiss, as much as possible, the interpretation of certain language - i will try to implement datatypes to prevent dynamically-type variables and try to remove the 'truthy' and 'falsy' of certain comparison :)

Kali is actually taken from one of the 5 largest island in Indonesia - Kalimantan or it is also known as Borneo. My simple reason on choosing Kali(mantan) is because, it is planned that the capital of Indonesia was planned to be moved to Borneo. I hope you guys got my reasoning here. It is stupid honestly.


To run the code, use the following command:
javac kali/Scanner.java && java kali.Kali <path-file>
javac kali/Scanner.java && java kali.Kali test.txt

AST generation
java tool/GenerateAst.java kali