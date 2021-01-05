# සිංSRL
## The First ever Semantic Role Labeler for Sinhala

This is the "සිංSRL", the first ever Sinhala Semantic Role Labeler that uses annotation projection method to transfer semantic roles from resource rich source language into resource poor target language Sinhala. This can easily adopt for other resource poor languages also. The "සිංSRL" follows the [projection approach by zalando research team](https://www.aclweb.org/anthology/D17-2008/).  

[සිංSRL Research Paper](http://www.colips.org/conferences/ialp2020/proceedings/papers/IALP2020_P51.pdf)

### Instructions to Setup the System
1. Host all the services in "Additional Tool" folder as micro services (All the instructions are given)
2. Add sentences into the input files `input.en` and `input.si`
3. Change the `serverAddress` in config.properties file according to the environment that the services are hosted
4. Run the Project
