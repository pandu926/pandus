

contract token{
    constructor() payable public{}
    fallback() payable external{}
     function testInCall(address callBAddress,address callCAddress, address toAddress ,uint256 amount,urcToken id) payable public{
         callBAddress.call(abi.encodeWithSignature("transC(address,address,uint256,urcToken)",callCAddress,toAddress,amount,id));
     }
    function testIndelegateCall(address callBddress,address callAddressC, address toAddress,uint256 amount, urcToken id) payable public{
         callBddress.delegatecall(abi.encodeWithSignature("transC(address,address,uint256,urcToken)",callAddressC,toAddress,amount,id));
     }
 }



contract B{
    constructor() public payable{}
    fallback() external payable{}
    function  transC(address callCAddress,address toAddress,uint256 amount, urcToken id) payable public{
         callCAddress.call(abi.encodeWithSignature("trans(address,uint256,urcToken)",toAddress,amount,id));
    }
}
contract C{
    constructor() payable public{}
    fallback() payable external{}
    function  trans(address payable toAddress,uint256 amount, urcToken id) payable public{
            toAddress.transferToken(amount,id);
    }

}
