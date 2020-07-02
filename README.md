# NUM Command Line Client

Download the latest [jar](https://github.com/NUMtechnology/num-cmd-line-client/releases) file and run like this:
```shell script
> alias num='java -jar num-cmd-line-client.jar'

# for the interactive version

> num

# or non-interactive

> num -uri num.uk:1

```

# Using JShell

If you have JDK9 or above installed you can use `jshell` which will allow you to use the up-arrow to select previous entries when using interactively:

```shell script

> jshell --class-path num-cmd-line-client.jar

jshell> import uk.num.cmd.Main;

jshell> Main.main(null);

Enter URI or Q[uit]>

```