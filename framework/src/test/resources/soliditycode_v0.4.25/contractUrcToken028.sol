pragma solidity ^0.4.24;

contract token{
    uint256 public a=1;
    constructor() public payable{}
    function tokenBalanceWithSameName(urcToken id) public payable{
        B b= new B();
        a= b.tokenBalance(id);
    }
    function getA() public returns(uint256){
        return a;
    }
}


contract B{
    uint256 public  flag =0;
    constructor() public payable{}
    function() public payable{}
    function tokenBalance(urcToken id) payable public returns(uint256){
        flag =9;
        return flag;
    }

}