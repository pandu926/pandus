

contract IllegalDecorate {

    constructor() payable public{}

    fallback() payable external{}

    function transferTokenWithView(address payable toAddress,urcToken id, uint256 tokenValue) public view{

        toAddress.transferToken(tokenValue, id);

    }

}