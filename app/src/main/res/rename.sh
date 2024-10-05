#!/bin/bash

# Define the directory path
folder_path="drawable"

# Character to replace and its replacement
old_char="-"
new_char="_"

# Iterate through all files in the directory
for file in "$folder_path"/*; do
    # Get the base name of the file (without the directory path)
    base_name=$(basename "$file")
    
    # Replace the character in the filename
    new_name="${base_name//$old_char/$new_char}"

    # Rename the file
    mv "$folder_path/$base_name" "$folder_path/$new_name"
done
