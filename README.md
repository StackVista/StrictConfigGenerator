StrictConfigGenerator
=====================

The generator transforms template file(s) into configuration files using variable values from a variable file.
Every variable in the template file(s) must present in the variables file.
Every variable in the variables file must be referenced at least once.

Command line parameters: <template file or directory> <variables file> <output dir>

- 1st param - The template file or directory containing template files.
            Variables to substitute shall be inserted in "##variable-name##" format.
            Example: 
            sever.address = ##server##
- 2nd param - The variables file containing the variables used for generating config in the "key = value" format.
            Example: server = http://google.com
- 3rd param - The output directory to which the config(s) should be generated.
