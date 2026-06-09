package main
import ("bufio";"fmt";"os";"sort";"strings")
func main(){
	uniq := false
	for _,a := range os.Args[1:] { if a=="-u" { uniq=true } }
	var lines []string
	sc := bufio.NewScanner(os.Stdin)
	for sc.Scan(){ lines = append(lines, sc.Text()) }
	sort.Strings(lines)
	if uniq {
		out := lines[:0]; var prev *string
		for i := range lines { if prev==nil || *prev != lines[i] { out=append(out,lines[i]); v:=lines[i]; prev=&v } }
		lines = out
	}
	fmt.Println(strings.Join(lines,"\n"))
}
