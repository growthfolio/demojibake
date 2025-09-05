package ioext

import (
	"bytes"
	"io"
	"io/fs"
	"os"
	"path/filepath"
)

var utf8BOM = []byte{0xEF, 0xBB, 0xBF}

func ReadSample(path string, n int) ([]byte, fs.FileInfo, error) {
	file, err := os.Open(path)
	if err != nil {
		return nil, nil, err
	}
	defer file.Close()

	info, err := file.Stat()
	if err != nil {
		return nil, nil, err
	}

	size := int(info.Size())
	if n > size {
		n = size
	}

	buf := make([]byte, n)
	bytesRead, err := file.Read(buf)
	if err != nil && err != io.EOF {
		return nil, nil, err
	}

	return buf[:bytesRead], info, nil
}

func OpenAtomicWrite(path string) (tmpPath string, f *os.File, cleanup func(), err error) {
	dir := filepath.Dir(path)
	base := filepath.Base(path)
	
	tmpPath = filepath.Join(dir, ".tmp_"+base)
	
	f, err = os.Create(tmpPath)
	if err != nil {
		return "", nil, nil, err
	}

	cleanup = func() {
		f.Close()
		os.Remove(tmpPath)
	}

	return tmpPath, f, cleanup, nil
}

func AtomicRename(tmpPath, finalPath string) error {
	return os.Rename(tmpPath, finalPath)
}

func StripUTF8BOM(b []byte) []byte {
	if bytes.HasPrefix(b, utf8BOM) {
		return b[3:]
	}
	return b
}

func AddUTF8BOM(b []byte) []byte {
	if !bytes.HasPrefix(b, utf8BOM) {
		return append(utf8BOM, b...)
	}
	return b
}

func HasUTF8BOM(b []byte) bool {
	return bytes.HasPrefix(b, utf8BOM)
}

func CopyWithTransform(dst io.Writer, src io.Reader, stripBOM, addBOM bool) error {
	const bufSize = 64 * 1024
	buf := make([]byte, bufSize)
	
	first := true
	for {
		n, err := src.Read(buf)
		if n > 0 {
			data := buf[:n]
			
			if first {
				if stripBOM {
					data = StripUTF8BOM(data)
				}
				if addBOM && !stripBOM {
					if _, writeErr := dst.Write(utf8BOM); writeErr != nil {
						return writeErr
					}
				}
				first = false
			}
			
			if _, writeErr := dst.Write(data); writeErr != nil {
				return writeErr
			}
		}
		
		if err == io.EOF {
			break
		}
		if err != nil {
			return err
		}
	}
	
	return nil
}