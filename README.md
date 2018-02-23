# AdvancementCommand
Runs a command, optionally as console or permission elevated when receiving advancements

## Documentation
See examples below for format

Each entry must define:
  - advancement
  - command OR alias

An advancement can be defined multiple times in the list to perform multiple commands.

If no other configuration is provided, it will be ran as the player without op or any elevated permissions.

Alias will alias one advancement to run the same commands as another, so you don't have to duplicate it.

### Keys
  - **advancement**: the advancement key such as `minecraft:brewed_potion` to trigger on
  - **command**: what command to run, see variables below for template variables
  - **run_as**: `player` or `console` - who receives the command feedback
  - **op**: Should player be given temporary op for this command. Unnecessary for `run_as: console`
  - **permission**: Permission node to give temporarily for this command.  Example: 
    ```yaml
    permission: minecraft.command.teleport
    ```
  - **permissions**: Same as above, but a list of permission nodes. Example:
    ```yaml
      permissions:
        - minecraft.command.teleport
        - some.other.perm
    ```
  - **alias**: Alias this advancement to run the same command information as another advancement defined here. You may still define permissions, run_as and op on the aliased entry, which will be added onto the permissions granted by the alias target.  
  In other words, you can alias foo to bar, where bar is defined to have permission b, and foo (the alias) is defined to have permission a, the command defined in bar will have both a and b permissions.
## Variables
Inside of a command, you may use the following variables
  - **%PLAYER**: The name of the player earning the advancement.
  - **%UUID**: The UUID of the player earning the advancement.
  - **%PLAYERDISPLAYNAME**: The display name of the player earning the advancement. (might be the same as player)
  - **%ADVANCEMENT**: The advancement key (eg `minecraft:brewed_potion`) being earned.

## Examples
```yaml
on_advancement:
  # example of running command temporarily as op - be careful, any sub commands of this command will have op too!
  - advancement: minecraft:example
    run_as: player
    command: ranks add %player rank1
    op: true
  # run a standard command with no permissions
  - advancement: minecraft:example
    run_as: player
    command: say Hey I just got an advancement!
    op: false
  # run a command as the console (command feedback to console instead of the player, ideal for rank changes)
  - advancement: myplugin:gainrank1
    run_as: console
    command: ranks add %player rank1
    op: false
  # run a command temporarily given a specific permission node, but not op
  - advancement: myplugin:teleport_to_secret
    run_as: player
    command: teleport %player some secret coords
    permission: minecraft.command.teleport
  - advancement: minecraft:brew_potion
    alias: myplugin:teleport_to_secret
  - advancement: minecraft:bred_animals
    alias: myplugin:teleport_to_secret
```

## Tricks
You may define fake advancement keys, to define a list of actions, and then use them
inside of your alias targets

## License
As with all my other public projects

Commands (c) Daniel Ennis (Aikar) 2018.

Commands is licensed [MIT](https://tldrlegal.com/license/mit-license). See [LICENSE](LICENSE)
