package ui

import (
	"fmt"
	"strings"
	"time"
)

type ProgressBar struct {
	total     int
	current   int
	width     int
	startTime time.Time
	lastDraw  time.Time
}

func NewProgressBar(total int) *ProgressBar {
	return &ProgressBar{
		total:     total,
		width:     50,
		startTime: time.Now(),
		lastDraw:  time.Now(),
	}
}

func (p *ProgressBar) Update(current int) {
	p.current = current
	
	// Throttle updates to avoid flickering
	if time.Since(p.lastDraw) < 100*time.Millisecond && current < p.total {
		return
	}
	p.lastDraw = time.Now()
	
	p.draw()
}

func (p *ProgressBar) Finish() {
	p.current = p.total
	p.draw()
	fmt.Println()
}

func (p *ProgressBar) draw() {
	percent := float64(p.current) / float64(p.total)
	filled := int(percent * float64(p.width))
	
	bar := strings.Repeat("█", filled) + strings.Repeat("░", p.width-filled)
	
	elapsed := time.Since(p.startTime)
	var eta string
	if p.current > 0 && p.current < p.total {
		rate := float64(p.current) / elapsed.Seconds()
		remaining := float64(p.total-p.current) / rate
		eta = fmt.Sprintf(" ETA: %v", time.Duration(remaining)*time.Second)
	}
	
	fmt.Printf("\r🔄 [%s] %d/%d (%.1f%%)%s", 
		bar, p.current, p.total, percent*100, eta)
}