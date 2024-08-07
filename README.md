# Razomy notation `.rn`

- data file
- free-text descriptions
- layering
- composition
- key-value pairs


### Sample

```rn
# value
value
# multiline value
`multiline
value`
# key:value
key:value
# key:multiline value
key:`multiline value`
key:`
multiline
value`
# multiline key:multiline value
`multiline key`:`default multiline value`
`
multiline
key
`:`
default
multiline
value
`
# inline key:key1:value
a:b
a:b:c
a:b:c:d
a:b;a1
a:b:c;;a1
a:b:c:d;;;a1
a:b:c:`
value
`
parent
 a:b:c:d:value
  child
 a:b:c;;a1
# escape key:key1:value
key\:
key\;
key\,
key:`\`
`
# tree
root
node:leaf
key  : default
    child
        key:value
key:default
    child:default
    `
    multiline
    child
    `:`default`
# comment
\# not comment
key:# not comment
key:\# not comment
key:default \# not comment
key:default \## not comment
key default
    # comment
    \# not comment
    child
# layers
layer
 property value
layer
 property2 value
```