package main
import ("crypto/sha256";"fmt";"io";"os")
func main(){
	b,_ := io.ReadAll(os.Stdin)
	fmt.Printf("%x\n", sha256.Sum256(b))
}
