//pragma solidity ^0.4.24;

 contract tokenTest{
     constructor() public payable{}
     function() external payable{}
     // positive case
     function TransferTokenTo(address payable toAddress, urcToken id,uint256 amount) public payable{
         //urcToken id = 0x74657374546f6b656e;
         toAddress.transferToken(amount,id);
     }
 }