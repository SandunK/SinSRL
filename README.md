# සිංSRL - The Projector

"[සිංSRL](http://www.colips.org/conferences/ialp2020/proceedings/papers/IALP2020_P51.pdf)" is the first ever Sinhala Semantic Role Labeler for the Sinhala language. This tool takes a Sinhala-Engish parallel sentence to output the Sinhala span based semantically annotated sentence. The tool facilitates us to *automatically generate linguistic resources (such as treebanks or propbanks) for the Sinhala language*, using a method referred to as annotation projection or annotation transfer.

Below an example of an English (top) and Sinhala (bottom) sentence pair. English annotations are automatically projected onto the Sinhala sentence, labeling it with named entities, semantic roles, and PoS tags.


![alt text](https://github.com/SandunK/SinSRL/blob/master/images/projection.jpg?raw=true)


### Getting Started

##### Dependencies
   - Java 8
   - Maven
 
##### Included Components

"සිංSRL" contains all the components to execute annotation projection in a set of simple, lightweight Java methods. It includes:

  1. *Syntactic and Semantic Parsers:* We wrap open source libraries such as StanfordNLP, ANNA, SinMorphy, Sinling, Sinhala-POS-Tagger, AllenNLP SRL Toolkit and Flair Predicate Identifier, so that syntactic parsing and semantic role labeling can be easily executed for Sinhala languages.
  2. *Word Alignment:* We use dictionary data for word alignment.
  3. *Annotation Transfer:* We provide an implementation of annotation transfer for a range of linguistic annotation, including part-of-speech tags, named entities, typed dependencies, and semantic roles.

 
##### Abstract architecture of the "සිංSRL" tool

![alt text](https://github.com/SandunK/SinSRL/blob/master/images/architecture.jpg?raw=true)


### Usage

##### Run "සිංSRL" tool

- Host all the services in "Additional Tool" folder as micro services (All the instructions are given)
- Add sentences into the input files `input.en` and `input.si`
- Change the `serverAddress` in config.properties file according to the environment that the services are hosted
- Run the Project
