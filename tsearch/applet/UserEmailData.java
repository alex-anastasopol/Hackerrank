// Autor: Razvan Popian  
// Email: razvan@cst.ro
package ro.cst.tsearch.applet;

import java.io.Serializable;

public class UserEmailData implements Serializable {
    
    static final long serialVersionUID = 10000000;
    
    public String userLoginId      = "";
    public String userName         = "";
    public String userEmailAddr    = "";

    public void setUserLoginId(String uli){
        userLoginId=uli;
    }

    public void setUserName(String un){
        userName=un;
    }

    public void setUserEmailAddr(String uea){
        userEmailAddr=uea;
    }
}
