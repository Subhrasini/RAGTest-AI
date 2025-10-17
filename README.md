# RAGTest-AI
An Agent for your test automation 
This project helps QA engineers and test automation developers save time, maintain consistency, and automatically extend their test coverage .

**Overview**
•	RAGTest-AI is an intelligent assistant that understands your Java Test automation code.

It uses Retrieval-Augmented Generation (RAG) to:
- Search through your codebase
- Answer natural-language questions about existing tests
- Generate new @Test/[Test] methods using context from your existing code

**Key Features**

•	Can generate test automation code in any language (like Java/.Net/Python/JavaScript) using any test framework (like TestNG,Nunit,BDD) .

•	Automatic Code Indexing – Scans and chunks .java files intelligently

•	RAG-based Code Retrieval – Uses semantic search via ChromaDB

•	Context-Aware Test Generation – Builds new tests from real examples

•	Smart Query Classification – Distinguishes between search vs generation

•	Multi-Step Planning – Decomposes complex user requests

•	Strict Output Control – Ensures only valid Java @Test method code

•	OpenSource

**How It Works**

1. The assistant loads your existing automation test code into a vector store (Chroma).
2. When you enter a request, it:
   - Classifies it (simple vs complex)
   - Generates a plan (if complex)
   - Retrieves relevant code snippets
   - Synthesizes a new test or explanation using Gemini 2.0 Flash.
Each response is grounded in your existing classes and methods, ensuring naming accuracy and context alignment.



*Please check wiki for setup details.



