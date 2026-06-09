package main
import ("encoding/json";"fmt";"io";"os")
func main(){
	b,_ := io.ReadAll(os.Stdin)
	var v any
	if err := json.Unmarshal(b,&v); err != nil { fmt.Fprintln(os.Stderr,"invalid JSON:",err); os.Exit(1) }
	out,_ := json.MarshalIndent(v,"","  ")
	fmt.Println(string(out))
}
