import { useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Typography,
  IconButton,
  Chip,
  MenuItem,
  Alert,
  Snackbar,
  FormControlLabel,
  Switch,
} from '@mui/material';
import { DataGrid, GridColDef, GridRenderCellParams } from '@mui/x-data-grid';
import {
  Add as AddIcon,
  Delete as DeleteIcon,
  Star as StarIcon,
  StarBorder as StarBorderIcon,
  CreditCard as CreditCardIcon,
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm, Controller } from 'react-hook-form';
import { paymentService, PaymentMethod, CreatePaymentMethodRequest } from '../api/paymentService';

const TYPE_OPTIONS = [
  { value: 'CREDIT_CARD', label: 'Carte de crédit', icon: '💳' },
  { value: 'DEBIT_CARD', label: 'Carte de débit', icon: '💳' },
  { value: 'STRIPE', label: 'Stripe', icon: '🔵' },
  { value: 'PAYPAL', label: 'PayPal', icon: '🅿️' },
];

export default function PaymentMethods() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedMethod, setSelectedMethod] = useState<PaymentMethod | null>(null);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' as 'success' | 'error' });

  const { register, handleSubmit, control, reset, watch, formState: { errors } } = useForm<CreatePaymentMethodRequest>();
  const selectedType = watch('type');

  const { data: methodsData, isLoading } = useQuery({
    queryKey: ['payment-methods', page, pageSize],
    queryFn: () => paymentService.getAllMethods(),
  });

  const createMutation = useMutation({
    mutationFn: (data: CreatePaymentMethodRequest) => paymentService.createMethod(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payment-methods'] });
      setDialogOpen(false);
      reset();
      setSnackbar({ open: true, message: 'Moyen de paiement créé', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Erreur lors de la création', severity: 'error' });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => paymentService.deleteMethod(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payment-methods'] });
      setDeleteDialogOpen(false);
      setSelectedMethod(null);
      setSnackbar({ open: true, message: 'Moyen de paiement supprimé', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Erreur lors de la suppression', severity: 'error' });
    },
  });

  const setDefaultMutation = useMutation({
    mutationFn: (id: number) => paymentService.setDefaultMethod(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payment-methods'] });
      setSnackbar({ open: true, message: 'Moyen de paiement par défaut mis à jour', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Erreur lors de la mise à jour', severity: 'error' });
    },
  });

  const getTypeConfig = (type: string) => {
    return TYPE_OPTIONS.find((t) => t.value === type) || TYPE_OPTIONS[0];
  };

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'userId', headerName: 'User ID', width: 90 },
    {
      field: 'type',
      headerName: 'Type',
      width: 150,
      renderCell: (params: GridRenderCellParams) => {
        const config = getTypeConfig(params.value);
        return (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <span>{config.icon}</span>
            <span>{config.label}</span>
          </Box>
        );
      },
    },
    { field: 'provider', headerName: 'Fournisseur', width: 130 },
    {
      field: 'lastFourDigits',
      headerName: 'Numéro',
      width: 120,
      valueFormatter: (value: string) => value ? `**** ${value}` : '-',
    },
    {
      field: 'expiry',
      headerName: 'Expiration',
      width: 100,
      valueGetter: (_, row: PaymentMethod) => 
        row?.expiryMonth && row?.expiryYear ? `${row.expiryMonth}/${row.expiryYear}` : '-',
    },
    {
      field: 'isDefault',
      headerName: 'Par défaut',
      width: 100,
      renderCell: (params: GridRenderCellParams) => (
        <IconButton
          size="small"
          onClick={() => !params.value && setDefaultMutation.mutate(params.row.id)}
          color={params.value ? 'warning' : 'default'}
        >
          {params.value ? <StarIcon /> : <StarBorderIcon />}
        </IconButton>
      ),
    },
    {
      field: 'isActive',
      headerName: 'Statut',
      width: 100,
      renderCell: (params: GridRenderCellParams) => (
        <Chip
          label={params.value ? 'Actif' : 'Inactif'}
          color={params.value ? 'success' : 'default'}
          size="small"
        />
      ),
    },
    {
      field: 'createdAt',
      headerName: 'Créé le',
      width: 120,
      valueFormatter: (value: string) => new Date(value).toLocaleDateString('fr-FR'),
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 100,
      sortable: false,
      renderCell: (params: GridRenderCellParams<PaymentMethod>) => (
        <Box>
          <IconButton
            size="small"
            color="error"
            onClick={() => {
              setSelectedMethod(params.row);
              setDeleteDialogOpen(true);
            }}
          >
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Box>
      ),
    },
  ];

  const onSubmit = (data: CreatePaymentMethodRequest) => {
    const payload: CreatePaymentMethodRequest = { ...data };
    if (data.cardNumber) {
      payload.lastFourDigits = data.cardNumber.replace(/\D/g, '').slice(-4);
    }
    delete payload.cardNumber;
    createMutation.mutate(payload);
  };

  const isCardType = selectedType === 'CREDIT_CARD' || selectedType === 'DEBIT_CARD';

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" fontWeight="bold">
          Moyens de paiement
        </Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogOpen(true)}>
          Ajouter
        </Button>
      </Box>

      <Card>
        <CardContent>
          <DataGrid
            rows={methodsData?.data || []}
            columns={columns}
            loading={isLoading}
            pageSizeOptions={[5, 10, 25]}
            paginationModel={{ page, pageSize }}
            onPaginationModelChange={(model) => {
              setPage(model.page);
              setPageSize(model.pageSize);
            }}
            autoHeight
            disableRowSelectionOnClick
          />
        </CardContent>
      </Card>

      {/* Create Dialog */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogTitle>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <CreditCardIcon />
              Ajouter un moyen de paiement
            </Box>
          </DialogTitle>
          <DialogContent>
            <TextField
              fullWidth
              label="User ID"
              type="number"
              margin="normal"
              {...register('userId', { required: 'User ID requis', valueAsNumber: true })}
              error={!!errors.userId}
              helperText={errors.userId?.message}
            />
            <Controller
              name="type"
              control={control}
              rules={{ required: 'Type requis' }}
              render={({ field }) => (
                <TextField
                  {...field}
                  select
                  fullWidth
                  label="Type"
                  margin="normal"
                  error={!!errors.type}
                  helperText={errors.type?.message}
                >
                  {TYPE_OPTIONS.map((option) => (
                    <MenuItem key={option.value} value={option.value}>
                      {option.icon} {option.label}
                    </MenuItem>
                  ))}
                </TextField>
              )}
            />
            <TextField
              fullWidth
              label="Fournisseur"
              margin="normal"
              {...register('provider', { required: 'Fournisseur requis' })}
              error={!!errors.provider}
              helperText={errors.provider?.message}
              placeholder={isCardType ? 'Visa, Mastercard, etc.' : 'Nom du service'}
            />
            {isCardType && (
              <>
                <TextField
                  fullWidth
                  label="Numéro de carte"
                  margin="normal"
                  {...register('cardNumber')}
                  placeholder="1234 5678 9012 3456"
                />
                <Box sx={{ display: 'flex', gap: 2 }}>
                  <TextField
                    label="Mois d'expiration"
                    type="number"
                    margin="normal"
                    {...register('expiryMonth', { valueAsNumber: true })}
                    inputProps={{ min: 1, max: 12 }}
                    sx={{ flex: 1 }}
                  />
                  <TextField
                    label="Année d'expiration"
                    type="number"
                    margin="normal"
                    {...register('expiryYear', { valueAsNumber: true })}
                    inputProps={{ min: 2024, max: 2040 }}
                    sx={{ flex: 1 }}
                  />
                </Box>
              </>
            )}
            <Controller
              name="isDefault"
              control={control}
              defaultValue={false}
              render={({ field }) => (
                <FormControlLabel
                  control={<Switch {...field} checked={field.value} />}
                  label="Définir comme moyen de paiement par défaut"
                  sx={{ mt: 2 }}
                />
              )}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setDialogOpen(false)}>Annuler</Button>
            <Button type="submit" variant="contained">
              Ajouter
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
        <DialogTitle>Confirmer la suppression</DialogTitle>
        <DialogContent>
          <Typography>
            Êtes-vous sûr de vouloir supprimer ce moyen de paiement ?
          </Typography>
          {selectedMethod?.lastFourDigits && (
            <Typography color="text.secondary" sx={{ mt: 1 }}>
              Carte se terminant par {selectedMethod.lastFourDigits}
            </Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)}>Annuler</Button>
          <Button
            color="error"
            variant="contained"
            onClick={() => selectedMethod && deleteMutation.mutate(selectedMethod.id)}
          >
            Supprimer
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={3000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
      >
        <Alert severity={snackbar.severity}>{snackbar.message}</Alert>
      </Snackbar>
    </Box>
  );
}
