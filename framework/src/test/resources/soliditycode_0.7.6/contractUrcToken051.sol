

 contract tokenTest{
     constructor() public payable{}
     fallback() external payable{}
     // positive case
     function TransferTokenTo(address payable toAddress, urcToken id,uint256 amount) public payable{
         //urcToken id = 0x74657374546f6b656e;
         toAddress.transferToken(amount,id);
     }
 }