package ec.com.megalujos.invoicing.dto;

public class DocumentFileDto {

    private String fileType;
    private String url;

    public DocumentFileDto() {
    }

    public DocumentFileDto(String fileType, String url) {
        this.fileType = fileType;
        this.url = url;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
