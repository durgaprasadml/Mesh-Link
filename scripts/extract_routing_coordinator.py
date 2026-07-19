import re

with open('app/src/main/java/com/meshlink/ble/data/BleRepositoryImpl.kt', 'r') as f:
    content = f.read()

# I will write a regex to find all methods to move.
# But it's easier to just do it manually with tools or a robust python script.
