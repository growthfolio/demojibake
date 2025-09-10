#!/usr/bin/env python3

import struct
import hashlib

def build_binary_dictionary(text_file, binary_file="dictionary_470k.bin"):
    """Constrói dicionário binário otimizado para 470k palavras"""
    
    entries = []
    string_buffer = b""
    
    with open(text_file, 'r', encoding='utf-8') as f:
        for line in f:
            if '|' not in line:
                continue
                
            correct, broken = line.strip().split('|', 1)
            
            # Adiciona strings ao buffer
            correct_offset = len(string_buffer)
            string_buffer += correct.encode('utf-8') + b'\0'
            
            broken_offset = len(string_buffer)
            string_buffer += broken.encode('utf-8') + b'\0'
            
            # Hash para lookup rápido
            broken_hash = hash(broken) & 0xFFFFFFFF
            
            entries.append({
                'broken_hash': broken_hash,
                'correct_offset': correct_offset,
                'broken_offset': broken_offset,
                'correct_len': len(correct.encode('utf-8')),
                'broken_len': len(broken.encode('utf-8'))
            })
    
    # Ordena por hash para binary search
    entries.sort(key=lambda x: x['broken_hash'])
    
    # Escreve arquivo binário
    with open(binary_file, 'wb') as f:
        # Header
        f.write(b'DICT')  # Magic
        f.write(struct.pack('<I', 1))  # Version
        f.write(struct.pack('<I', len(entries)))  # Num entries
        f.write(struct.pack('<I', len(string_buffer)))  # String buffer size
        
        # Entries (20 bytes each)
        for entry in entries:
            f.write(struct.pack('<IIIHH', 
                entry['broken_hash'],
                entry['correct_offset'],
                entry['broken_offset'],
                entry['correct_len'],
                entry['broken_len']
            ))
        
        # String buffer
        f.write(string_buffer)
    
    print(f"Dicionário binário criado: {len(entries)} entradas")
    print(f"Tamanho: {len(string_buffer) // 1024}KB strings + {len(entries) * 20 // 1024}KB índice")

if __name__ == "__main__":
    build_binary_dictionary("dictionary_470k.txt")