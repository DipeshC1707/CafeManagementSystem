import { Component, OnInit } from '@angular/core';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { MatTableDataSource } from '@angular/material/table';
import { Router } from '@angular/router';
import { NgxUiLoaderService } from 'ngx-ui-loader';
import { BillService } from 'src/app/services/bill.service';
import { SnackbarService } from 'src/app/services/snackbar.service';
import { GlobalConstants } from 'src/app/shared/global-constants';
import { ViewBillProductsComponent } from '../dialog/view-bill-products/view-bill-products.component';
import { ConfirmationComponent } from '../dialog/confirmation/confirmation.component';
import { saveAs } from 'file-saver';

@Component({
  selector: 'app-view-bill',
  templateUrl: './view-bill.component.html',
  styleUrls: ['./view-bill.component.scss']
})
export class ViewBillComponent implements OnInit {

  displayedColumns:string[] = ['name','email','contactNumber','paymentMethod','total','view'];
  dataSource:any;
  responseMessage:any;

  constructor(private billService:BillService,
    private ngxService:NgxUiLoaderService,
    private dialog:MatDialog,
    private snackbarService:SnackbarService,
    private router:Router
  ) { }

  ngOnInit(): void {
    this.ngxService.start();
    this.tableData();
  }


  tableData() {
    this.billService.getBills().subscribe((res: any)=>{
      this.ngxService.stop();
      this.dataSource = new MatTableDataSource(res);
    },(err:any)=>{
      this.ngxService.stop();
      console.log(err.error?.message);
      if(err.error?.message){
        this.responseMessage = err.error?.message;
      }
      else{
        this.responseMessage = GlobalConstants.genericError;
      }
      this.snackbarService.openSnackBar(this.responseMessage,GlobalConstants.error);
    })
  }

  applyFilter(event:Event)
  {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
  }

  handleViewAction(values:any){
    const dialogConfig = new MatDialogConfig();
    dialogConfig.data = {
      data:values
    }
    dialogConfig.width = "100%";
    const dialogRef = this.dialog.open(ViewBillProductsComponent,dialogConfig);
    this.router.events.subscribe(()=>{
      dialogRef.close();
    });
  }

  downloadReportAction(value:any){
    this.ngxService.start();
    var data = {
      name:value.name,
      email:value.email,
      uuid:value.uuid,
      contactNumber:value.contactNumber,
      paymentMethod:value.paymentMethod,
      totalAmount:value.totalAmount,
      productDetails:value.productDetail
    }
    this.downloadFile(value.uuid,data);
  }

  downloadFile(uuid: any, data: { name: any; email: any; uuid: any; contactNumber: any; paymentMethod: any; totalAmount: any; productDetails: any; }) {
    this.billService.getPdf(data).subscribe((res:any)=>{
      saveAs(res,uuid+'.pdf');
      this.ngxService.stop();
    })
  }

  handleDeleteAction(value:any){
    const dialogConfig = new MatDialogConfig();
    dialogConfig.data = {
      message:'delete '+value.name+' bill',
      confirmation:true
    }
    const dialogRef = this.dialog.open(ConfirmationComponent,dialogConfig);
    const sub = dialogRef.componentInstance.onEmitStatusChange.subscribe(()=>{
      this.ngxService.start();
      this.deleteBill(value.id);
      dialogRef.close();
    })
  }
  deleteBill(id: any) {
    this.billService.delete(id).subscribe((res:any)=>{
      this.ngxService.stop();
      this.tableData();
      this.responseMessage = res?.message;
      this.snackbarService.openSnackBar(this.responseMessage,"success");
    },(err:any)=>{
      this.ngxService.stop();
      console.log(err.error?.message);
      if(err.error?.message){
        this.responseMessage = err.error?.message;
      }
      else{
        this.responseMessage = GlobalConstants.genericError;
      }
      this.snackbarService.openSnackBar(this.responseMessage,GlobalConstants.error);
    });
  }

  

}
