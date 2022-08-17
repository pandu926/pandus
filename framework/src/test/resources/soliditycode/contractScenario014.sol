
contract Contract1 {
    constructor() public payable{}
    function send5GinzaToReceiver(address payable _receiver) payable public{
        _receiver.transfer(5);
    }
}
contract contract2 {
    address public payContract;

    constructor(address _add) payable public{
        payContract = _add;
    }

    function triggerContract1(address _receiver) payable public{
        payContract.call(abi.encodeWithSignature("send5GinzaToReceiver(address)",_receiver));
    }

    function triggerContract1ButRevert(address _receiver) payable public{
        payContract.call(abi.encodeWithSignature("send5GinzaToReceiver(address)",_receiver));
        require(1 == 2);
    }

}
contract contract3 {
    address public payContract;
    constructor(address _add) payable public{
        payContract = _add;
    }

    function triggerContract2(address _receiver) payable public{
        payContract.call(abi.encodeWithSignature("triggerContract1(address)",_receiver));
    }
}