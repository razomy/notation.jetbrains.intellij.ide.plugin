# Razomy notation `.rn`

- data file
- free-text descriptions
- layering
- composition
- key-value pairs


### Sample

```rn
#comment
# value
value
# multiline value
`
multiline
value
`
# key:value
key:value
# key:multiline value
key:`
multiline value
`
key:`
multiline
value
`
# multiline key:multiline value
`
multiline
key
`:`
default
multiline
value
`
# multiline recursion
`
`1
`2
`3
3`
2`
1`
`
# iterate inline
a,b,c,d
# nesting key:key1:value
a:b
a:b:c
# 1 space nesting
a
 b
  c
# split nesting
a:b;a1
a:b:c;;a1
a:b:c:d;;;a1
a:b:c:`
value
`
# mixed
parent
 a:b:c:d:value
  child
 a:b:c;;a1
# escape key:key1:value
key\:
key\;
key\,
key\'
key:`
\`
`
# in tree terms
root
 node:leaf
 key:default
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
# reference
a:b
c:.a.b
# injection
json_inject:json:`
{
 "a": 1,
 "b": "2"
}
`
js_inject:js:`
a=1
`
rn_recursion_inject:rn:`
key:rn:`1
key:json:`2
{"a":1}
2`
1`
`
```